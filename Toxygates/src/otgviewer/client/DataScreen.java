/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.ClusteringSelector;
import otgviewer.client.components.ListChooser;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.client.components.TickMenuItem;
import otgviewer.shared.Group;
import t.common.shared.clustering.ProbeClustering;
import t.viewer.client.table.ExpressionTable;
import t.viewer.client.table.RichTable.HideableColumn;
import t.viewer.shared.ItemList;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
	protected ExpressionTable et;
	protected ClusteringSelector cs;

	protected String[] lastProbes;
	protected List<Group> lastColumns;
	
	public DataScreen(ScreenManager man) {
		super("View data", key, true, man,
				resources.dataDisplayHTML(), resources.dataDisplayHelp());
		et = makeExpressionTable();
		cs = makeClusteringSelector();
		cs.setAvailable(ProbeClustering.createFrom(appInfo().predefinedProbeLists()));
	}

	private ClusteringSelector makeClusteringSelector() {
		return new ClusteringSelector() {
			@Override
			public void clusterChanged(List<String> items) {
				updateProbes(items);
			}
		};
	}

	protected ExpressionTable makeExpressionTable() {
		return new ExpressionTable(this);
	}

	@Override 
	protected void addToolbars() {
		super.addToolbars();
		HorizontalPanel mainTools = new HorizontalPanel();
		mainTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		mainTools.add(et.tools());
		if (addSelector()) {
			mainTools.add(cs.selector());			
		}
		addToolbar(mainTools, 43);
		addToolbar(et.analysisTools(), 43);
	}
	
	protected boolean addSelector() {
		return true;
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
		for (final HideableColumn c : et.getHideableColumns()) {
			new TickMenuItem(mb, c.columnInfo().title(), c.visible()) {
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
								appInfo().predefinedProbeLists(),
								 "probes") {
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
	
	private void updateProbes(List<String> items) {
		lastProbes = null;
		lastColumns = null;
		
		changeProbes(items.toArray(new String[0]));
		
		StorageParser p = getParser(this);
		storeProbes(p);

		show();
	}
	
}
