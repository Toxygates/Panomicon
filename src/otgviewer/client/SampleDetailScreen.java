package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.Annotation;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SampleDetailScreen extends Screen {

	public static final String key = "ad";
	
	private CellTable<String[]> experimentTable = new CellTable<String[]>();
	private CellTable<String[]> biologicalTable = new CellTable<String[]>();
	
	private ScrollPanel sp = new ScrollPanel();
	private VerticalPanel vp = new VerticalPanel();
	private ListBox columnList = new ListBox();
	
	private Barcode[] barcodes;
	private DataColumn displayColumn;
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	AnnotationTDGrid atd = new AnnotationTDGrid();
	
	public SampleDetailScreen(ScreenManager man) {
		super("Sample details", key, false, man);						
		this.addListener(atd);
	}
	
	@Override
	public void columnsChanged(List<DataColumn> columns) {
		super.columnsChanged(columns);
		if (columns.size() > 0) {
			displayColumn = chosenColumns.get(0);

			columnList.clear();
			for (DataColumn c : chosenColumns) {
				columnList.addItem(c.getShortTitle());
			}
			columnList.setSelectedIndex(0);
			displayWith(columnList.getItemText(columnList.getSelectedIndex()));
		}
	}

	@Override
	public void customColumnChanged(DataColumn customColumn) {
		Window.alert("Changed");		
		super.customColumnChanged(customColumn);
		if (customColumn != null) {
			columnList.addItem(customColumn.getShortTitle());
			columnList.setSelectedIndex(columnList.getItemCount() - 1);
			displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			storeCustomColumn(null); // consume the data so it doesn't turn up
										// again.
		}
	}

	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key);
	}

	public Widget content() {
		HorizontalPanel hp = Utils.mkHorizontalPanel();
		dockPanel.add(hp, DockPanel.NORTH);
		
		hp.add(columnList);
		
		hp.add(new Button("Grid visualisation...", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				Set<String> compounds = new HashSet<String>();
				for (DataColumn d: chosenColumns) {
					compounds.addAll(Arrays.asList(((Group) d).getCompounds()));
				}
				List<String> compounds_ = new ArrayList<String>(compounds);
				atd.compoundsChanged(compounds_);
				Utils.displayInPopup(atd);								
			}
		}));
		
		columnList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent ce) {
				displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			}
		});

		vp.add(sp);
		
		VerticalPanel vpi = new VerticalPanel();
		vpi.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		vpi.add(experimentTable);
		vpi.add(biologicalTable);
		sp.setWidget(vpi);		
		
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
		while(experimentTable.getColumnCount() > 0) {
			experimentTable.removeColumn(0);
		}
		while(biologicalTable.getColumnCount() > 0) {
			biologicalTable.removeColumn(0);
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
		experimentTable.addColumn(col, "Experiment detail");
		experimentTable.setColumnWidth(col, "15em");
		biologicalTable.addColumn(col, "Biological detail");
		biologicalTable.setColumnWidth(col, "15em");
		for (int i = 1; i < barcodes.length + 1; ++i) {
			String name = barcodes[i - 1].getCode().substring(2); //remove leading 00
			TextColumn<String[]> c = makeColumn(i);
			experimentTable.addColumn(c, name);		
			experimentTable.setColumnWidth(c, "10em");			
			biologicalTable.addColumn(c, name);
			biologicalTable.setColumnWidth(c, "10em");
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
					List<String[]> annotations = new ArrayList<String[]>();
					
					final int numEntries = as[0].getEntries().size();
					int i = 0;
					while(i < numEntries && i < 23) {
						String[] item = new String[barcodes.length + 1];
						item[0] = as[0].getEntries().get(i).description;
						for (int j = 0; j < as.length; ++j) {					
							item[j + 1] = as[j].getEntries().get(i).value;						
						}
						annotations.add(item);
						i += 1;
					}
					experimentTable.setRowData(annotations);
					annotations.clear();
					
					while(i < numEntries) {
						String[] item = new String[barcodes.length + 1];
						item[0] = as[0].getEntries().get(i).description;
						for (int j = 0; j < as.length; ++j) {					
							item[j + 1] = as[j].getEntries().get(i).value;						
						}
						annotations.add(item);
						i += 1;
					}
					biologicalTable.setRowData(annotations);
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
