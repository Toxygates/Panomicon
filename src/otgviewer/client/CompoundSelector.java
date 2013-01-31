package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.StringSelectionTable;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Pair;
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
import com.google.gwt.view.client.NoSelectionModel;

/**
 * This widget is for selecting a compound or a set of 
 * compounds using various data sources.
 * 
 * Receives: dataFilter
 * Emits: compounds
 * @author johan
 *
 */
public class CompoundSelector extends DataListenerWidget implements RequiresResize {

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	private static Resources resources = GWT.create(Resources.class);
	
	private StringSelectionTable compoundTable;
	private DockLayoutPanel dp;
	private boolean hasRankColumns = false;
	private Button sortButton;
	
	private Map<String, Double> scores = new HashMap<String, Double>(); //for compound ranking
	private List<String> rankProbes = new ArrayList<String>();
	
	/**
	 * @wbp.parser.constructor
	 */
	public CompoundSelector() {
		this("Compounds");
	}
	
	public CompoundSelector(String heading) {
		
		dp = new DockLayoutPanel(Unit.EM);

		initWidget(dp);
		Label lblCompounds = new Label(heading);
		lblCompounds.setStyleName("heading");
		dp.addNorth(lblCompounds, 2.5);
	
		HorizontalPanel hp = Utils.mkWidePanel();		
		dp.addSouth(hp, 2.5);
		
		sortButton = new Button("Sort by name", new ClickHandler() {
			public void onClick(ClickEvent ce) {
				loadCompounds();
				sortButton.setEnabled(false);
			}
		});
		hp.add(sortButton);
		sortButton.setEnabled(false);
		
		hp.add(new Button("Unselect all", new ClickHandler() {
			public void onClick(ClickEvent ce) {
				setSelection(new ArrayList<String>());
			}
		}));
		
		compoundTable = new StringSelectionTable("Sel", "Compound") {
			protected void selectionChanged(Set<String> selected) {
				List<String> r = new ArrayList<String>();
				r.addAll(selected);
				Collections.sort(r);
				changeCompounds(r);
			}
		
		};		
				
		dp.add(new ScrollPanel(compoundTable));
		
		compoundTable.setWidth("100%");
		compoundTable.table().setSelectionModel(new NoSelectionModel<String>());		
	}
	
	private void addRankColumns() {
		if (!hasRankColumns) {
			CellTable<String> table = compoundTable.table();
 			TextColumn<String> textColumn = new TextColumn<String>() {
				@Override
				public String getValue(String object) {
					if (scores.containsKey(object)) {
						return Utils.formatNumber(scores.get(object));					
					} else {
						return "N/A";
					}
				}
			};
			table.addColumn(textColumn, "Score");
			
			ChartClickCell ccc = new ChartClickCell(this);
			table.addColumn(new IdentityColumn<String>(ccc), "");
			hasRankColumns = true;
		}
	}
	
	private void removeRankColumns() {
		if (hasRankColumns) {
			CellTable<String> table = compoundTable.table();
			table.removeColumn(3); //chart icons
			table.removeColumn(2); //score
			rankProbes.clear();
			scores.clear();
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
		loadCompounds();
		compoundTable.clearSelection();				
	}
	
	public List<String> getCompounds() {				
		List<String> r = new ArrayList<String>();
		r.addAll(compoundTable.selection());		
		Collections.sort(r);
		return r;
	}

	void loadCompounds() {		
		owlimService.compounds(chosenDataFilter, new PendingAsyncCallback<String[]>(this, "Unable to retrieve compounds") {
			
			@Override
			public void handleSuccess(String[] result) {
				Arrays.sort(result);
				List<String> r = new ArrayList<String>((Arrays.asList(result)));
				compoundTable.reloadWith(r, true);				
				changeAvailableCompounds(Arrays.asList(result));								
			}
			
		});
	}
	
	public void setSelection(List<String> compounds) {		
		compoundTable.clearSelection();		
		compoundTable.selectAll(compounds);
		
		compoundTable.table().redraw();		
		Collections.sort(compounds);
		changeCompounds(compounds);
	}
	
	
	
	@Override
	public void onResize() {		
		dp.onResize();		
	}

	void performRanking(List<String> rankProbes, List<RankRule> rules) {
		this.rankProbes = rankProbes;
		addRankColumns();
		
		if (rules.size() > 0) { //do we have at least 1 rule?						
			owlimService.rankedCompounds(chosenDataFilter, rules.toArray(new RankRule[0]),
					new PendingAsyncCallback<Pair<String, Double>[]>(this) {
						public void handleSuccess(Pair<String, Double>[] res) {
							List<String> sortedCompounds = new ArrayList<String>();
							for (Pair<String, Double> p : res) {
								scores.put(p.first(), p.second());
								sortedCompounds.add(p.first());
							}									
							compoundTable.reloadWith(sortedCompounds, false);		
							sortButton.setEnabled(true);
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
		
		public void onClick(String value) {
			if (rankProbes.size() == 0) {
				Window.alert("These charts can only be displayed if compounds have been ranked.");
			} else {
				kcService.getSeries(chosenDataFilter, rankProbes.toArray(new String[0]), 
						null, new String[] { value }, new PendingAsyncCallback<List<Series>>(w, "Unable to retrieve data.") {
					public void handleSuccess(List<Series> ss) {
						SeriesChartGrid scg = new SeriesChartGrid(chosenDataFilter, ss, false);
						Utils.displayInPopup(scg);
					}
					
				});
			}
		}
	}
}
