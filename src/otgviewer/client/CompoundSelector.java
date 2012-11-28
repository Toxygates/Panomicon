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
import otgviewer.shared.RuleType;
import otgviewer.shared.Series;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
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
public class CompoundSelector extends DataListenerWidget {

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	
	private StringSelectionTable compoundTable;
	private ScrollPanel scrollPanel;
	private VerticalPanel verticalPanel;
	
	//compound sorter widgets
	private VerticalPanel csVerticalPanel = new VerticalPanel();
	private final int RANK_CONDS = 10;
	private TextBox[] sortProbeText = new TextBox[RANK_CONDS];
	private ListBox[] rankType = new ListBox[RANK_CONDS];
	private ListBox[] rankRefCompound = new ListBox[RANK_CONDS];
	private ListBox[] rankRefDose = new ListBox[RANK_CONDS];
	private TextBox[] syntheticCurveText = new TextBox[RANK_CONDS];
	private CheckBox[] rankCheckBox = new CheckBox[RANK_CONDS];
	private List<String> rankProbes = new ArrayList<String>();	
	
	private Map<String, Double> scores = new HashMap<String, Double>(); //for compound ranking
	
	/**
	 * @wbp.parser.constructor
	 */
	public CompoundSelector() {
		this("Compounds");
	}
	
	public CompoundSelector(String heading) {
		makeCompoundSorter();
		
		verticalPanel = new VerticalPanel();
		initWidget(verticalPanel);
		verticalPanel.setWidth("100%");
		verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		
		Label lblCompounds = new Label(heading);
		lblCompounds.setStyleName("heading");
		verticalPanel.add(lblCompounds);
		
		Button b = new Button("Sort by effect on genes...");
		verticalPanel.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				displayCompoundSorter();
			}
		});
		
		scrollPanel = new ScrollPanel();
		verticalPanel.add(scrollPanel);
		scrollPanel.setSize("100%", "400px");
		
		b = new Button("Unselect all");
		verticalPanel.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				setSelection(new ArrayList<String>());
			}
		});
		
		final DataListenerWidget w = this;
		compoundTable = new StringSelectionTable("Sel", "Compound") {
			protected void selectionChanged(Set<String> selected) {
				List<String> r = new ArrayList<String>();
				r.addAll(selected);
				Collections.sort(r);
				changeCompounds(r);
			}
			
			protected void initTable(CellTable<String> table) {
				super.initTable(table);
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
				
				ChartClickCell ccc = new ChartClickCell(w);
				table.addColumn(new IdentityColumn<String>(ccc), "");
			}
		};
		scrollPanel.setWidget(compoundTable);
		compoundTable.setSize("100%", "100%");
		compoundTable.table().setSelectionModel(new NoSelectionModel<String>());		
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);		
		loadCompounds();
		compoundTable.clearSelection();		
		for (ListBox lb: rankRefCompound) {
			lb.clear();
		}
	}
	
	public List<String> getCompounds() {				
		List<String> r = new ArrayList<String>();
		r.addAll(compoundTable.selection());		
		Collections.sort(r);
		return r;
	}

	void loadCompounds() {		
		owlimService.compounds(chosenDataFilter, new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				Arrays.sort(result);
				List<String> r = new ArrayList<String>((Arrays.asList(result)));
				compoundTable.reloadWith(r, true);			
				
				for (ListBox lb: rankRefCompound) {
					for (String c: result) {
						lb.addItem(c);
					}
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to retrieve compounds");			
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
	
	int lastHeight = 0;
	@Override
	public void heightChanged(int newHeight) {
		if (newHeight != lastHeight) {
			lastHeight = newHeight;
			super.heightChanged(newHeight);
			verticalPanel.setHeight((newHeight - verticalPanel.getAbsoluteTop() - 50) + "px");
			scrollPanel.setHeight((newHeight - scrollPanel.getAbsoluteTop() - 70) + "px");
		}
	}
	
	void displayCompoundSorter() {
		Utils.displayInPopup(csVerticalPanel);		
	}
	
	private void makeRankRuleInputs(Grid g, int row) {
		sortProbeText[row] = new TextBox();
		rankType[row] = new ListBox();
		for (RuleType rt: RuleType.values()) {
			rankType[row].addItem(rt.toString());
		}
		rankCheckBox[row] = new CheckBox();
		syntheticCurveText[row] = new TextBox();
		syntheticCurveText[row].setWidth("10em");
		rankRefCompound[row] = new ListBox();	
		rankRefCompound[row].setStyleName("colored");
		ListBox lb = new ListBox();
		rankRefDose[row] = lb;
		lb.setStyleName("colored");
		lb.addItem("Low");
		lb.addItem("Middle");
		lb.addItem("High");
		
		g.setWidget(row + 1, 0, rankCheckBox[row]);
		g.setWidget(row + 1, 1, sortProbeText[row]);
		g.setWidget(row + 1, 2, rankType[row]);
		g.setWidget(row + 1, 3, syntheticCurveText[row]);
		g.setWidget(row + 1, 4, rankRefCompound[row]);
		g.setWidget(row + 1, 5, rankRefDose[row]);		
	}
	
	private void makeCompoundSorter() {
		csVerticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label l = new Label("Compound ranking");
		l.setStyleName("heading");
		csVerticalPanel.add(l);
		
		Grid g = new Grid(RANK_CONDS + 1, 6);
		csVerticalPanel.add(g);
		g.setWidget(0, 1, Utils.mkEmphLabel("Gene/probe"));
		g.setWidget(0, 2, Utils.mkEmphLabel("Match type"));
		g.setWidget(0, 3, Utils.mkEmphLabel("Synthetic curve"));
		g.setWidget(0, 4, Utils.mkEmphLabel("Ref. compound"));
		g.setWidget(0, 5, Utils.mkEmphLabel("Ref. dose"));		
		
		for (int row = 0; row < RANK_CONDS; row++) {
			makeRankRuleInputs(g, row);			
		}		
		
		Button b = new Button("Rank");
		csVerticalPanel.add(b);
		
		b.addClickHandler(new ClickHandler() {						
			public void onClick(ClickEvent event) {
				performRanking();				
			}
		});		
	}
	
	private void performRanking() {
		List<RankRule> rules = new ArrayList<RankRule>();
		rankProbes = new ArrayList<String>();
		for (int i = 0; i < RANK_CONDS; ++i) {
			if (rankCheckBox[i].getValue()) {
				if (!sortProbeText[i].getText().equals("")) {
					String probe = sortProbeText[i].getText();
					rankProbes.add(probe);
					RuleType rt = RuleType.valueOf(rankType[i]
							.getItemText(rankType[i].getSelectedIndex()));
					
					if (rt == RuleType.Synthetic) {
						double[] data = new double[4];
						String[] ss = syntheticCurveText[i].getText()
								.split(" ");
						RankRule r = new RankRule(rt, probe);
						if (ss.length != 4) {
							Window.alert("Please supply 4 space-separated values as the synthetic curve. (Example: -1 -2 -3 -4");							
						} else {
							for (int j = 0; j < ss.length; ++j) {
								data[j] = Double.valueOf(ss[j]);
							}
							r.setData(data);
							rules.add(r);
						}
					} else if (rt == RuleType.ReferenceCompound) {						
							String cmp = rankRefCompound[i]
									.getItemText(rankRefCompound[i].getSelectedIndex());
							RankRule r = new RankRule(rt, probe);
							r.setCompound(cmp);
							r.setDose(rankRefDose[i].getItemText(rankRefDose[i].getSelectedIndex()));
							rules.add(r);
						
					} else {
						//rule is not synthetic or ref compound
						rules.add(new RankRule(rt, probe));
					}
				} else {
					//gene name is not empty
					Window.alert("Empty gene name detected. Please specify a gene/probe for each enabled rule.");
				}
			}
		}
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
			super("chart_16.png");
			this.w = w;
		}
		
		public void onClick(String value) {
			kcService.getSeries(chosenDataFilter, rankProbes.toArray(new String[0]), 
					null, new String[] { value }, new PendingAsyncCallback<List<Series>>(w) {
				public void handleSuccess(List<Series> ss) {
					SeriesChartGrid scg = new SeriesChartGrid(ss, false);
					Utils.displayInPopup(scg);
				}
				public void handleFailure(Throwable caught) {
					Window.alert("Unable to retrieve data.");
				}
			});
		}
	}
}
