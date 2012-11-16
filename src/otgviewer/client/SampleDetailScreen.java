package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.ScreenManager;
import otgviewer.shared.Annotation;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SampleDetailScreen extends Screen {

	public static final String key = "ad";
	
	private CellTable<String[]> annotationTable = new CellTable<String[]>();
	private ScrollPanel sp = new ScrollPanel();
	private VerticalPanel vp = new VerticalPanel();
	private List<String[]> annotations = new ArrayList<String[]>();
	private Barcode[] barcodes;
	private DataColumn displayColumn, customColumn;
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	
	public SampleDetailScreen(DataColumn column, Screen parent, ScreenManager man) {
		super(parent, "Sample details", key, false, man);		
		customColumn = column;
		displayColumn = customColumn;		
		parent.propagateTo(this); //nonstandard data flow
		if (displayColumn == null) {
			displayColumn = chosenColumns.get(0);
		}
	}
	
	public Widget content() {
		HorizontalPanel hp = new HorizontalPanel();
		dockPanel.add(hp, DockPanel.NORTH);
		
		final ListBox columnList = new ListBox();
		hp.add(columnList);
		
		columnList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent ce) {
				displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			}
		});
		
		for (DataColumn c: chosenColumns) {
			columnList.addItem(c.getShortTitle());
		}		
		if (customColumn != null) {
			columnList.addItem(customColumn.getShortTitle());
			columnList.setSelectedIndex(columnList.getItemCount() - 1);
		} else {
			columnList.setSelectedIndex(0);			
		}		
		displayWith(columnList.getItemText(columnList.getSelectedIndex()));
				
		vp.add(sp);
		sp.setWidget(annotationTable);		
		
		return vp;
	}
	
	
	private TextColumn<String[]> makeColumn(final int idx) {
		return new TextColumn<String[]>() {
			public String getValue(String[] x) {
				return x[idx];
			}
		};
	}
	
	private void displayWith(String column) {		
		while(annotationTable.getColumnCount() > 0) {
			annotationTable.removeColumn(0);
		}
		
		if (customColumn != null && column.equals(customColumn.getShortTitle())) {
			barcodes = customColumn.getBarcodes();
			displayColumn = customColumn;
		} else {
			for (DataColumn c : chosenColumns) {
				if (c.getShortTitle().equals(column)) {
					barcodes = c.getBarcodes();
					displayColumn = c;
				}
			}
		}
		
		TextColumn<String[]> col = new TextColumn<String[]>() {
			public String getValue(String[] x) {
				return x[0];											
			}
		}; 
		annotationTable.addColumn(col, "Annotation");		
		for (int i = 1; i < barcodes.length + 1; ++i) {			
			annotationTable.addColumn(makeColumn(i), barcodes[i - 1].getCode().substring(2)); //remove leading 00
		}
		reload();
	}

	private void reload() {
		if (displayColumn != null) {
			owlimService.annotations(displayColumn, new AsyncCallback<Annotation[]>() {
				public void onFailure(Throwable caught) {
					Window.alert("Unable to get array annotations.");
				}
				public void onSuccess(Annotation[] as) {
					annotations.clear();

					final int numEntries = as[0].getEntries().size();
					for (int i = 0; i < numEntries; i++) {
						String[] item = new String[barcodes.length + 1];
						item[0] = as[0].getEntries().get(i).description;
						for (int j = 0; j < as.length; ++j) {					
							item[j + 1] = as[j].getEntries().get(i).value;						
						}
						annotations.add(item);
					}

					annotationTable.setRowData(annotations);
				}
			});
		}
	}
	
	int lastHeight = 0;
	@Override
	public void heightChanged(int newHeight) {
		if (newHeight != lastHeight) {
			lastHeight = newHeight;
			super.heightChanged(newHeight);
			vp.setHeight((newHeight - vp.getAbsoluteTop() - 50) + "px");
			sp.setHeight((newHeight - sp.getAbsoluteTop() - 70) + "px");
		}
	}

}
