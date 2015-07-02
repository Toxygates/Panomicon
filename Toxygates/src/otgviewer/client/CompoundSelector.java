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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.charts.ChartGrid;
import otgviewer.client.charts.Charts;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.MatchResult;
import otgviewer.shared.RankRule;
import otgviewer.shared.Series;
import t.common.shared.ItemList;
import t.common.shared.SampleClass;
import t.common.shared.StringList;
import t.viewer.client.Utils;
import t.viewer.client.components.StackedListEditor;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SeriesServiceAsync;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * This widget is for selecting a compound or a set of 
 * compounds using various data sources.
 * It can also request compound ranking and display the results of such a ranking.
 * 
 * 
 * Receives: dataFilter
 * Emits: compounds
 * @author johan
 *
 */
public class CompoundSelector extends DataListenerWidget implements RequiresResize {

	private final SparqlServiceAsync sparqlService;
	private final SeriesServiceAsync seriesService;
	private final Resources resources;
	
	private StackedListEditor compoundEditor;
	private DockLayoutPanel dp;
	private boolean hasRankColumns = false;
	
	private Map<String, Integer> ranks = new HashMap<String, Integer>(); //for compound ranking
	private Map<String, MatchResult> scores = new HashMap<String, MatchResult>(); //for compound ranking
	private List<String> rankProbes = new ArrayList<String>();
	
	private Widget north;
	private Screen screen;
	private final String majorParameter;

	private final static int MAX_AUTO_SEL = 20;
	
	public CompoundSelector(final Screen screen, String heading, 
	    boolean withListSelector, boolean withFreeEdit) {
		this.screen = screen;
		this.sparqlService = screen.sparqlService();
		this.seriesService = screen.seriesService();
		this.resources = screen.resources();
		
		dp = new DockLayoutPanel(Unit.PX);
		this.majorParameter = screen.schema().majorParameter();
		
		initWidget(dp);
		Label lblCompounds = new Label(heading);
		lblCompounds.setStylePrimaryName("heading");
		dp.addNorth(lblCompounds, 40);
		north = lblCompounds;
		
		//TODO factor out
		boolean isAdjuvant = screen.manager().getUIType().equals("adjuvant");
		
		final Collection<StringList> predefLists = 
				(isAdjuvant ? TemporaryCompoundLists.predefinedLists() 
						: new ArrayList<StringList>());
		
		compoundEditor = new StackedListEditor(this, "compounds", heading, 
				MAX_AUTO_SEL, predefLists, withListSelector, withFreeEdit) {
			@Override
			protected void selectionChanged(Set<String> selected) {
				List<String> r = new ArrayList<String>();
				r.addAll(selected); 
				Collections.sort(r);
				changeCompounds(r);
			}		
			
			@Override
			protected void listsChanged(List<ItemList> itemLists) {
				screen.itemListsChanged(itemLists);
				screen.storeItemLists(getParser(screen));
			}
		};		
		
		compoundEditor.displayPicker();
		dp.add(compoundEditor);	
		compoundEditor.table().setSelectionModel(new NoSelectionModel<String>());		
	}
	
	private void addRankColumns() {
		if (!hasRankColumns) {
			CellTable<String> table = compoundEditor.table();
 			TextColumn<String> textColumn = new TextColumn<String>() {
				@Override
				public String getValue(String object) {
					String r = "";
					
					if (scores.containsKey(object)) {
						r += Utils.formatNumber(scores.get(object).score());					
					} else {
						r += "N/A";
					}
					if (ranks.containsKey(object)) {
						r += " (" + ranks.get(object) + ")";
					}
					return r;
				}
			};
			table.addColumn(textColumn, "Score");			
			
			ChartClickCell ccc = new ChartClickCell(this);
			IdentityColumn<String> clickCol = new IdentityColumn<String>(ccc);
			clickCol.setCellStyleNames("clickCell");
			table.addColumn(clickCol, "");
			
			hasRankColumns = true;
		}
	}
	
	private void removeRankColumns() {
		if (hasRankColumns) {
			CellTable<String> table = compoundEditor.table();
			table.removeColumn(3); //chart icons
			table.removeColumn(2); //score
			rankProbes.clear();
			scores.clear();
			ranks.clear();
			hasRankColumns = false;
		}
	}
	
	private SampleClass lastClass;
	
	@Override
	public void sampleClassChanged(SampleClass sc) {
		super.sampleClassChanged(sc);				
		if (lastClass == null || !sc.equals(lastClass)) {
			removeRankColumns();
		}
		lastClass = sc;
		
		screen.enqueue(new Screen.QueuedAction("loadCompounds") {			
			@Override
			public void run() {
				loadMajors();
				compoundEditor.clearSelection();				
			}
		});		
	}
	
	@Override
	public void itemListsChanged(List<ItemList> lists) {
		super.itemListsChanged(lists);
		compoundEditor.setLists(lists);
	}

	public List<String> getCompounds() {				
		List<String> r = new ArrayList<String>();
		r.addAll(compoundEditor.getSelection());		
		Collections.sort(r);
		return r;
	}

	void loadMajors() {				
		sparqlService.parameterValues(chosenSampleClass, majorParameter, 
				new PendingAsyncCallback<String[]>(this, 
						"Unable to retrieve values for parameter: " + majorParameter) {
			
			@Override
			public void handleSuccess(String[] result) {
				Arrays.sort(result);
				List<String> r = new ArrayList<String>((Arrays.asList(result)));
				compoundEditor.setItems(r, true, true);
				changeAvailableCompounds(Arrays.asList(result));								
			}			
		});
	}
	
	public void setSelection(List<String> compounds) {						
		compoundEditor.setSelection(compounds);
			
		Collections.sort(compounds);
		changeCompounds(compounds);
	}
	
	@Override
	public void onResize() {	
		// Since this is not a ResizeComposite, we need to pass on this signal manually
		dp.onResize();		
	}
	
	public void resizeInterface() {
		dp.setWidgetSize(north, 40);	
	}

	public void performRanking(List<String> rankProbes, List<RankRule> rules) {
		this.rankProbes = rankProbes;
		addRankColumns();
		
		if (rules.size() > 0) { //do we have at least 1 rule?						
			seriesService.rankedCompounds(chosenDatasets, chosenSampleClass, 
					rules.toArray(new RankRule[0]),
					new PendingAsyncCallback<MatchResult[]>(this) {
						public void handleSuccess(MatchResult[] res) {
							ranks.clear();
							int rnk = 1;
							List<String> sortedCompounds = new ArrayList<String>();
							for (MatchResult r : res) {
								scores.put(r.compound(), r);
								sortedCompounds.add(r.compound());
								ranks.put(r.compound(), rnk);
								rnk++;
							}									
							compoundEditor.setItems(sortedCompounds, false, false);
							compoundEditor.displayPicker();
						}

						public void handleFailure(Throwable caught) {
							Window.alert("Unable to rank compounds: " + caught.getMessage());							
						}
					});									
		} else {
			Window.alert("Please specify and enable at least one rule to perform the ranking.");
		}
	}
	
	class ChartClickCell extends ImageClickCell.StringImageClickCell {
		final DataListenerWidget w;
		public ChartClickCell(DataListenerWidget w) {
			super(resources.chart(), false);
			this.w = w;
		}
		
		public void onClick(final String value) {
			if (rankProbes.size() == 0) {
				Window.alert("These charts can only be displayed if compounds have been ranked.");
			} else {
				seriesService.getSeries(chosenSampleClass, rankProbes.toArray(new String[0]), 
						null, new String[] { value }, getSeriesCallback(value));					
			}
		}
		
		private AsyncCallback<List<Series>> getSeriesCallback(final String value) {
			 return new PendingAsyncCallback<List<Series>>(w, "Unable to retrieve data.") {
					public void handleSuccess(final List<Series> ss) {
						Utils.ensureVisualisationAndThen(new Runnable() {
							public void run() {
								makeSeriesCharts(value, ss);		
							}
						});
							
					}
			 };
		}
		
		private void makeSeriesCharts(final String value, final List<Series> ss) {
			Charts cgf = new Charts(screen, 
					new SampleClass[] { w.chosenSampleClass });
			cgf.makeSeriesCharts(ss, false, scores.get(value).dose(),
					new Charts.ChartAcceptor() {
				
				@Override
				public void acceptCharts(ChartGrid<?> cg) {
					Utils.displayInPopup("Charts", cg, DialogPosition.Side);								
				}
			}, screen);			
		}
	}
}
