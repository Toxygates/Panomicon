package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import bioweb.shared.array.Annotation;
import bioweb.shared.array.DataColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * This screen displays detailed information about a sample or a set of samples,
 * i.e. experimental conditions, histopathological data, blood composition.
 * The samples that can be displayed are the currently configured groups.
 * In addition, a single custom group of samples can be passed to this screen 
 * (the "custom column") to make it display samples that are not in the configured groups.
 * @author johan
 *
 */
public class SampleDetailScreen extends Screen {
	
	public static final String key = "ad";
	
	private CellTable<String[]> experimentTable = new CellTable<String[]>();
	private CellTable<String[]> biologicalTable = new CellTable<String[]>();
	
	private ListBox columnList = new ListBox();
	
	private Barcode[] barcodes;
	private BarcodeColumn displayColumn;
	private SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);
	AnnotationTDGrid atd = new AnnotationTDGrid(this);
	
	private DataFilter lastFilter;
	private List<Group> lastColumns;
	private BarcodeColumn lastCustomColumn;
	
	private HorizontalPanel tools;
	
	public SampleDetailScreen(ScreenManager man) {
		super("Sample details", key, true, true, man);						
		this.addListener(atd);
		mkTools();
	}
	
	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		if (visible && !columns.equals(lastColumns)) {
			updateColumnList();
		}
	}
	
	private void updateColumnList() {
		columnList.clear();
		if (chosenColumns.size() > 0) {
			setDisplayColumn(chosenColumns.get(0));
			columnList.setSelectedIndex(0);
			for (DataColumn c : chosenColumns) {
				columnList.addItem(c.getShortTitle());
			}
		}
		if (chosenCustomColumn != null) {
			columnList.addItem(chosenCustomColumn.getShortTitle());
			columnList.setSelectedIndex(columnList.getItemCount() - 1);
		}
	}

	@Override
	public void show() {
		super.show();
		if (visible
				&& (lastFilter == null || !lastFilter.equals(chosenDataFilter)
						|| lastColumns == null
						|| !chosenColumns.equals(lastColumns) || chosenCustomColumn != null)
						|| chosenCustomColumn != null && (lastCustomColumn == null || !lastCustomColumn
								.equals(chosenCustomColumn))) {
			updateColumnList();			
			displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			
			lastFilter = chosenDataFilter;
			lastColumns = chosenColumns;						
			lastCustomColumn = chosenCustomColumn;
		}
	}

	@Override
	public void customColumnChanged(BarcodeColumn customColumn) {
		super.customColumnChanged(customColumn);
		if (visible) {
			updateColumnList();
			storeCustomColumn(null); // consume the data so it doesn't turn
										// up again.
		}
	}

	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key);
	}

	private void mkTools() {
		HorizontalPanel hp = Utils.mkHorizontalPanel(true);				
		tools = Utils.mkWidePanel();		
		tools.add(hp);

		hp.add(columnList);
		
		hp.add(new Button("Grid visualisation...", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				Set<String> compounds = new HashSet<String>();
				for (BarcodeColumn d: chosenColumns) {
					compounds.addAll(Arrays.asList(((Group) d).getCompounds()));
				}
				List<String> compounds_ = new ArrayList<String>(compounds);
				atd.compoundsChanged(compounds_);
				Utils.displayInPopup("Visualisation", atd);								
			}
		}));
		
		columnList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent ce) {
				displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			}
		});		
	}
	
	public Widget content() {
		
//		vp.add(hpi);
		
		VerticalPanel vp = Utils.mkVerticalPanel();
		configureTable(vp, experimentTable);
		configureTable(vp, biologicalTable);

		HorizontalPanel hp = Utils.mkWidePanel(); //to make it centered
		hp.add(vp);
		return new ScrollPanel(hp);				
	}
	
	private void configureTable(Panel p, CellTable<String[]> ct) {
		ct.setWidth("100%", true); //use fixed layout so we can control column width explicitly
		ct.setSelectionModel(new NoSelectionModel<String[]>());
		ct.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		p.add(ct);
	}

	private void setDisplayColumn(BarcodeColumn c) {
		barcodes = c.getSamples();
		displayColumn = c;
	}
	
	private void displayWith(String column) {
		while(biologicalTable.getColumnCount() > 0) {
			biologicalTable.removeColumn(0);
		}
		while(experimentTable.getColumnCount() > 0) {
			experimentTable.removeColumn(0);
		}
		
		displayColumn = null;
		if (chosenCustomColumn != null && column.equals(chosenCustomColumn.getShortTitle())) {
			setDisplayColumn(chosenCustomColumn);
		} else {
			for (BarcodeColumn c : chosenColumns) {
				if (c.getShortTitle().equals(column)) {
					setDisplayColumn(c);					
				}
			}
		}
		if (displayColumn == null) {
			Window.alert("Error: no display column selected.");
		}
		
		makeColumn(experimentTable, 0, "Experiment detail", "15em");
		makeColumn(biologicalTable, 0, "Biological detail", "15em");		
		
		for (int i = 1; i < barcodes.length + 1; ++i) {
			String name = barcodes[i - 1].getCode().substring(2); //remove leading 00
			makeColumn(biologicalTable, i, name, "9em");					
			makeColumn(experimentTable, i, name, "9em");
		}
		biologicalTable.setWidth((15 + 9 * barcodes.length) + "em", true);
		experimentTable.setWidth((15 + 9 * barcodes.length) + "em", true);
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

	private String[] makeAnnotItem(int i, Annotation[] as) {
		String[] item = new String[barcodes.length + 1];
		item[0] = as[0].getEntries().get(i).description;
//		Window.alert(item.length + " " + as.length + " " + barcodes.length);
		for (int j = 0; j < as.length; ++j) {					
			item[j + 1] = as[j].getEntries().get(i).value;						
		}
		return item;
	}
	
	private void reload() {
		if (displayColumn != null) {
			owlimService.annotations(displayColumn, new PendingAsyncCallback<Annotation[]>(this) {
				public void handleFailure(Throwable caught) {
					Window.alert("Unable to get array annotations.");
				}
				public void handleSuccess(Annotation[] as) {
					List<String[]> annotations = new ArrayList<String[]>();
					
					final int numEntries = as[0].getEntries().size();
					int i = 0;
					while(i < numEntries && i < 23) {
						annotations.add(makeAnnotItem(i, as));						
						i += 1;
					}
					experimentTable.setRowData(annotations);
					annotations.clear();
					
					while(i < numEntries) {
						annotations.add(makeAnnotItem(i, as));						
						i += 1;
					}
					biologicalTable.setRowData(annotations);
				}
			});
		}
	}

	@Override
	protected void addToolbars() {	
		super.addToolbars();
		addToolbar(tools, 30);
	}
	
}
