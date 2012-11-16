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
import otgviewer.client.components.StringSelectionTable;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Pair;
import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;
import otgviewer.shared.Series;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
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
	private TextBox[] sortProbeText = new TextBox[5];
	private ListBox[] rankType = new ListBox[5];
	private TextBox[] syntheticCurveText = new TextBox[5];
	private List<String> rankProbes = new ArrayList<String>();
	
	private Map<String, Double> scores = new HashMap<String, Double>(); //for compound ranking
	
	/**
	 * @wbp.parser.constructor
	 */
	public CompoundSelector() {
		this("Compounds");
	}
	
	public CompoundSelector(String heading) {
//		chosenDataFilter = initFilter;
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
				
				ChartClickCell ccc = new ChartClickCell();
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
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to retrieve compounds");			
			}
		});
	}
	
	public void setSelection(List<String> compounds) {		
		compoundTable.clearSelection();		
		compoundTable.selection().addAll(compounds);
		
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
	
	private void makeCompoundSorter() {
		csVerticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label l = new Label("Compound ranking");
		l.setStyleName("heading");
		csVerticalPanel.add(l);
		
		Grid g = new Grid(6, 3);
		csVerticalPanel.add(g);
		g.setWidget(0, 0, new Label("Gene/probe"));
		g.setWidget(0, 1, new Label("Match type"));
		g.setWidget(0, 2, new Label("Synthetic curve"));
		
		for (int i = 0; i < 5; i++) {
			sortProbeText[i] = new TextBox();
			rankType[i] = new ListBox();
			rankType[i].addItem("Sum");
			rankType[i].addItem("Negative sum");			
			rankType[i].addItem("Synthetic curve");
			rankType[i].addItem("Low variance");
			rankType[i].addItem("High variance");
			rankType[i].addItem("Unchanged");
			rankType[i].addItem("Strict increasing");
			rankType[i].addItem("Strict decreasing");
			syntheticCurveText[i] = new TextBox();
			g.setWidget(i + 1, 0, sortProbeText[i]);
			g.setWidget(i + 1, 1, rankType[i]);
			g.setWidget(i + 1, 2, syntheticCurveText[i]);
		}		
		
		Button b = new Button("Rank");
		csVerticalPanel.add(b);
		
		b.addClickHandler(new ClickHandler() {						
			public void onClick(ClickEvent event) {
				RankRule[] rules = new RankRule[5];
				rankProbes = new ArrayList<String>();
				for (int i = 0; i < 5; ++i) {
					if (!sortProbeText[i].getText().equals("")) {
						String probe = sortProbeText[i].getText();
						rankProbes.add(probe);
						switch (rankType[i].getSelectedIndex()) {
						case 0:
							rules[i] = new RankRule(RuleType.Sum, probe);
							break;
						case 1:
							rules[i] = new RankRule(RuleType.NegativeSum, probe);
							break;						
						case 2:
							double[] data = new double[4];
							String[] ss = syntheticCurveText[i].getText()
									.split(" ");
							if (ss.length != 4) {
								Window.alert("Please supply 4 space-separated values as the synthetic curve. (Example: -1 -2 -3 -4");
							} else {
								for (int j = 0; j < ss.length; ++j) {
									data[j] = Double.valueOf(ss[j]);
								}
								rules[i] = new RankRule(RuleType.Synthetic, probe);
								rules[i].setData(data);
							}
							break;
						case 3:
							rules[i] = new RankRule(RuleType.LowVariance, probe);
							break;
						case 4:
							rules[i] = new RankRule(RuleType.HighVariance, probe);
							break;
						case 5:
							rules[i] = new RankRule(RuleType.Unchanged, probe);
							break;
						case 6:
							rules[i] = new RankRule(RuleType.MonotonicUp, probe);
							break;
						case 7:
							rules[i] = new RankRule(RuleType.MonotonicDown, probe);
							break;
						}
					}
				}
				if (rules[0] != null) {
					owlimService.rankedCompounds(chosenDataFilter, rules,
							new AsyncCallback<Pair<String, Double>[]>() {
								public void onSuccess(Pair<String, Double>[] res) {
									List<String> sortedCompounds = new ArrayList<String>();
									for (Pair<String, Double> p : res) {
										scores.put(p.first(), p.second());
										sortedCompounds.add(p.first());
									}
									compoundTable.reloadWith(sortedCompounds, false);									
								}

								public void onFailure(Throwable caught) {
									Window.alert("Unable to rank compounds.");
								}
							});
										
				}
			}
		});
		
	}
	
	class ChartClickCell extends ImageClickCell {
		public ChartClickCell() {
			super("chart_16.png");
		}
		
		public void onClick(String value) {
			kcService.getSeries(chosenDataFilter, rankProbes.toArray(new String[0]), 
					null, value, new AsyncCallback<List<Series>>() {
				public void onSuccess(List<Series> ss) {
					SeriesChartGrid scg = new SeriesChartGrid(ss);
					Utils.displayInScrolledPopup(scg);
				}
				public void onFailure(Throwable caught) {
					Window.alert("Unable to retrieve data.");
				}
			});
		}
	}
}
