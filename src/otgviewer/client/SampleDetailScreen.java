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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;

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
		super("Sample details", key, true, true, man);						
		this.addListener(atd);
	}
	
	@Override
	public void columnsChanged(List<DataColumn> columns) {
		super.columnsChanged(columns);
		if (visible) {
			if (columns.size() > 0) {
				displayColumn = chosenColumns.get(0);

				columnList.clear();
				for (DataColumn c : chosenColumns) {
					columnList.addItem(c.getShortTitle());
				}
				columnList.setSelectedIndex(0);
				displayWith(columnList.getItemText(columnList
						.getSelectedIndex()));
			}
		}
	}

	@Override
	public void show() {
		super.show();
		columnsChanged(chosenColumns);
		customColumnChanged(chosenCustomColumn);		
	}

	@Override
	public void customColumnChanged(DataColumn customColumn) {
		super.customColumnChanged(customColumn);
		if (visible) {
			if (customColumn != null) {
				columnList.addItem(customColumn.getShortTitle());
				columnList.setSelectedIndex(columnList.getItemCount() - 1);
				displayWith(columnList.getItemText(columnList
						.getSelectedIndex()));
				storeCustomColumn(null); // consume the data so it doesn't turn
											// up
											// again.
			}
		}
	}

	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key);
	}

	public Widget content() {
		HorizontalPanel hp = Utils.mkHorizontalPanel(true);		
		HorizontalPanel hpi = Utils.mkHorizontalPanel();
		hpi.setWidth("100%");
		hpi.add(hp);
		vp.add(hpi);
		
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
		
		configureTable(vp, experimentTable);
		configureTable(vp, biologicalTable);				
		return vp;
	}
	
	private void configureTable(Panel p, CellTable<String[]> ct) {
		ct.setWidth("100%", true); //use fixed layout so we can control column width explicitly
		ct.setSelectionModel(new NoSelectionModel());
		ct.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		p.add(ct);
	}

	private void displayWith(String column) {
		while(biologicalTable.getColumnCount() > 0) {
			biologicalTable.removeColumn(0);
		}
		while(experimentTable.getColumnCount() > 0) {
			experimentTable.removeColumn(0);
		}
		
		if (chosenCustomColumn != null && column.equals(chosenCustomColumn.getShortTitle())) {
			barcodes = chosenCustomColumn.getBarcodes();
			displayColumn = chosenCustomColumn;
		} else {
			for (DataColumn c : chosenColumns) {
				if (c.getShortTitle().equals(column)) {
					barcodes = c.getBarcodes();
					displayColumn = c;
				}
			}
		}
		
		makeColumn(experimentTable, 0, "Experiment detail", "15em");
		makeColumn(biologicalTable, 0, "Biological detail", "15em");		
		
		for (int i = 1; i < barcodes.length + 1; ++i) {
			String name = barcodes[i - 1].getCode().substring(2); //remove leading 00
			makeColumn(biologicalTable, i, name, "9em");					
			makeColumn(experimentTable, i, name, "9em");
		}
		reload();
	}
	
	private TextColumn<String[]> makeColumn(CellTable<String[]> table, final int idx, String title, String width) {
		TextColumn<String[]> col = new TextColumn<String[]>() {
			public String getValue(String[] x) {
				if (x.length > idx) {
					return x[idx];
				} else {
					return "";
				}
			}
		};
		
		table.addColumn(col, title);
		table.setColumnWidth(col, width);
		return col;
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

}
