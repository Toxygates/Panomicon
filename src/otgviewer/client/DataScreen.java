package otgviewer.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.components.ListChooser;
import otgviewer.client.components.RichTable.HideableColumn;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.TickMenuItem;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import t.viewer.shared.ItemList;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The main data display screen.
 * Data is displayed in the ExpressionTable widget.
 * @author johan
 *
 */
public class DataScreen extends Screen {

	public static final String key = "data";
	private ExpressionTable et;
		
	private String[] lastProbes;
	private List<Group> lastColumns;
	
	public DataScreen(ScreenManager man) {
		super("View data", key, true, man,
				resources.dataDisplayHTML(), resources.dataDisplayHelp());
		et = makeExpressionTable();
	}
	
	protected ExpressionTable makeExpressionTable() {
		return new ExpressionTable(this);
	}
	
	@Override
	protected void addToolbars() {
		super.addToolbars();
		addToolbar(et.tools(), 43);
		addToolbar(et.analysisTools(), 43);	
	}

	public Widget content() {		
		addListener(et);		
		setupMenuItems();
		ResizeLayoutPanel rlp = new ResizeLayoutPanel();
		rlp.setWidth("100%");
		rlp.add(et);
		return rlp;		
	}
	
	private void setupMenuItems() {
		MenuBar mb = new MenuBar(true);		
		MenuItem mActions = new MenuItem("File", false, mb);		
		final DataScreen w = this;
		MenuItem mntmDownloadCsv = new MenuItem("Download CSV (grouped samples)...", false, new Command() {
			public void execute() {
				et.downloadCSV(false);
				
			}
		});
		mb.addItem(mntmDownloadCsv);
		 mntmDownloadCsv = new MenuItem("Download CSV (individual samples)...", false, new Command() {
				public void execute() {
					et.downloadCSV(true);
					
				}
			});
			mb.addItem(mntmDownloadCsv);
		
		addMenu(mActions);
		
		mb = new MenuBar(true);
		for (final HideableColumn c: et.getHideableColumns()) {
			new TickMenuItem(mb, c.name(), c.visible()) {
				@Override
				public void stateChange(boolean newState) {
					et.setVisible(c, newState);
				}				
			};
		}
		
		MenuItem mColumns = new MenuItem("View", false, mb);
		addMenu(mColumns);		
		
		addAnalysisMenuItem(new MenuItem("Save visible genes as list...",
				new Command() {
					public void execute() {
						// Create an invisible listChooser that we exploit only for
						// the sake of saving a new list.
						ListChooser lc = new ListChooser(
								new HashMap<String, List<String>>(), "probes") {
							@Override
							protected void listsChanged(List<ItemList> lists) {
								w.itemListsChanged(lists);
								w.storeItemLists(w.getParser());
							}
						};
						// TODO make ListChooser use the DataListener propagate mechanism?
						lc.setLists(chosenItemLists);
						lc.setItems(Arrays.asList(et.displayedAtomicProbes()));
						lc.saveAction();
					}
				}));
		
		// TODO: this is effectively a tick menu item without the tick.
		// It would be nice to display the tick graphic, but then the textual alignment
		// of the other items on the menu becomes odd.
		addAnalysisMenuItem(
				new TickMenuItem("Compare two sample groups", false, false) {
					public void stateChange(boolean newState) {						
						if (!visible) {
							//Trigger screen
							manager.attemptProceed(DataScreen.key);
							setState(true);							
							showToolbar(et.analysisTools());
						} else {			
							//Just toggle
							if (newState) {
								showToolbar(et.analysisTools());
							} else {
								hideToolbar(et.analysisTools());
							}
						}
					}
				}.menuItem());			
				
	}
	
	@Override
	public boolean enabled() {
		return manager.isConfigured(ProbeScreen.key) && manager.isConfigured(ColumnScreen.key); 
	}

	public void show() {
		super.show();
		//state has finished loading
		
		logger.info("chosenProbes: " + chosenProbes.length +
				" lastProbes: " + (lastProbes == null ? "null" : "" + lastProbes.length));
		
		// Attempt to avoid reloading the data
		if (lastColumns == null || !chosenColumns.equals(lastColumns)) {
			logger.info("Data reloading needed");
			et.getExpressions(); 
		} else if (!Arrays.equals(chosenProbes, lastProbes)) {
			logger.info("Only refiltering is needed");
			et.refilterData();
		}

		lastProbes = chosenProbes;		
		lastColumns = chosenColumns;
	}
	
	@Override
	public String getGuideText() {
		return "Here you can inspect expression values for the sample groups you have defined. Click on column headers to sort data.";
	}

	@Override
	public void probesChanged(String[] probes) {
		super.probesChanged(probes);
		logger.info("received " + probes.length + " probes");
	}
	
}
