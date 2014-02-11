package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import bioweb.shared.array.DataColumn;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

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
	
	private SampleDetailTable experimentTable = new SampleDetailTable(this, "Experiment detail");
	private SampleDetailTable biologicalTable = new SampleDetailTable(this, "Biological detail");
	
	private ListBox columnList = new ListBox();

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
			for (DataColumn<?> c : chosenColumns) {
				columnList.addItem(c.getShortTitle());
			}
		}
		if (chosenCustomColumn != null) {
			columnList.addItem(chosenCustomColumn.getShortTitle());
			columnList.setSelectedIndex(columnList.getItemCount() - 1);
		} else {
			columnList.setSelectedIndex(0);
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
			StorageParser p = getParser(this);
			if (p != null) {
				// consume the data so it doesn't turn up again.
				storeCustomColumn(p, null); 
			}
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
		
		hp.add(new Button("Heatmap...", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				Set<String> compounds = new HashSet<String>();
				for (BarcodeColumn d: chosenColumns) {
					compounds.addAll(Arrays.asList(((Group) d).getCompounds()));
				}
				List<String> compounds_ = new ArrayList<String>(compounds);
				atd.compoundsChanged(compounds_);
				Utils.displayInPopup("Visualisation", atd, DialogPosition.Center);								
			}
		}));
		
		columnList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent ce) {
				displayWith(columnList.getItemText(columnList.getSelectedIndex()));
			}
		});		
	}
	
	public Widget content() {		
		VerticalPanel vp = Utils.mkVerticalPanel(false, experimentTable, biologicalTable);

		HorizontalPanel hp = Utils.mkWidePanel(); //to make it centered
		hp.add(vp);
		return new ScrollPanel(hp);				
	}
	
	
	private void setDisplayColumn(BarcodeColumn c) {
		experimentTable.loadFrom(c, false, 0, 23);
		biologicalTable.loadFrom(c, false, 23, -1);
	}
	
	private void displayWith(String column) {		
		if (chosenCustomColumn != null && column.equals(chosenCustomColumn.getShortTitle())) {
			setDisplayColumn(chosenCustomColumn);
			return;
		} else {
			for (BarcodeColumn c : chosenColumns) {
				if (c.getShortTitle().equals(column)) {
					setDisplayColumn(c);
					return;
				}
			}
		}
		Window.alert("Error: no display column selected.");
	}

	@Override
	protected void addToolbars() {	
		super.addToolbars();
		addToolbar(tools, 30);
	}
	
	@Override
	public String getGuideText() {
		return "Here you can view experimental information and biological details for each sample in the groups you have defined.";
	}	
}
