package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.charts.ChartGrid;
import otgviewer.client.charts.ChartGridFactory;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.StackedListEditor;
import otgviewer.shared.DataFilter;
import otgviewer.shared.MatchResult;
import otgviewer.shared.RankRule;
import otgviewer.shared.Series;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
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

	private SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);
	private SeriesServiceAsync seriesService = (SeriesServiceAsync) GWT
			.create(SeriesService.class);
	private static Resources resources = GWT.create(Resources.class);
	
	private StackedListEditor compoundEditor;
	private DockLayoutPanel dp;
	private boolean hasRankColumns = false;
	
	private Map<String, Integer> ranks = new HashMap<String, Integer>(); //for compound ranking
	private Map<String, MatchResult> scores = new HashMap<String, MatchResult>(); //for compound ranking
	private List<String> rankProbes = new ArrayList<String>();
	
	private Widget north;
	private Screen screen;
	
	/**
	 * @wbp.parser.constructor
	 */
	public CompoundSelector() {
		this(null, "Compounds");
	}

	public CompoundSelector(Screen screen, String heading) {
		this.screen = screen;
		dp = new DockLayoutPanel(Unit.EM);

		initWidget(dp);
		Label lblCompounds = new Label(heading);
		lblCompounds.setStyleName("heading");
		dp.addNorth(lblCompounds, 2.5);
		north = lblCompounds;
		
		compoundEditor = new StackedListEditor("Compound", 
				TemporaryCompoundLists.predefinedLists()) {
			@Override
			protected void selectionChanged(Set<String> selected) {
				List<String> r = new ArrayList<String>();
				r.addAll(selected);
				Collections.sort(r);
				changeCompounds(r);
			}
		
		};		
				
		dp.add(compoundEditor);
		
//		compoundEditor.setWidth("300px");
//		compoundEditor.setHeight("500px");
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
	
	private DataFilter lastFilter;
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);

		if (lastFilter == null || !filter.equals(lastFilter)) {
			removeRankColumns();
		}
		lastFilter = filter;
		
		screen.enqueue(new Screen.QueuedAction("loadCompounds") {			
			@Override
			public void run() {
				loadCompounds();
				compoundEditor.clearSelection();				
			}
		});
		
	}
	
	public List<String> getCompounds() {				
		List<String> r = new ArrayList<String>();
		r.addAll(compoundEditor.getSelection());		
		Collections.sort(r);
		return r;
	}

	void loadCompounds() {		
		sparqlService.compounds(chosenDataFilter, new PendingAsyncCallback<String[]>(this, "Unable to retrieve compounds") {
			
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
		dp.setWidgetSize(north, 2.5);	
//		compoundEditor.resizeInterface();
	}

	void performRanking(List<String> rankProbes, List<RankRule> rules) {
		this.rankProbes = rankProbes;
		addRankColumns();
		
		if (rules.size() > 0) { //do we have at least 1 rule?						
			seriesService.rankedCompounds(chosenDataFilter, rules.toArray(new RankRule[0]),
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
	
	class ChartClickCell extends ImageClickCell {
		final DataListenerWidget w;
		public ChartClickCell(DataListenerWidget w) {
			super(resources.chart());
			this.w = w;
		}
		
		public void onClick(final String value) {
			if (rankProbes.size() == 0) {
				Window.alert("These charts can only be displayed if compounds have been ranked.");
			} else {
				seriesService.getSeries(chosenDataFilter, rankProbes.toArray(new String[0]), 
						null, new String[] { value }, new PendingAsyncCallback<List<Series>>(w, "Unable to retrieve data.") {
					public void handleSuccess(final List<Series> ss) {
						Utils.ensureVisualisationAndThen(new Runnable() {
							public void run() {
								ChartGridFactory cgf = new ChartGridFactory(chosenDataFilter, chosenColumns);
								cgf.makeSeriesCharts(ss, false, scores.get(value).dose(), new ChartGridFactory.ChartAcceptor() {
									
									@Override
									public void acceptCharts(ChartGrid cg) {
										Utils.displayInPopup("Charts", cg, DialogPosition.Side);								
									}
								}, screen);					
							}
						});
							
					}
					
				});
			}
		}
	}
}
