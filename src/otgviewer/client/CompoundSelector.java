package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.shared.DataFilter;
import otgviewer.shared.Pair;
import otgviewer.shared.RankRule;
import scala.swing.Table;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
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
	
	private CellTable<String> compoundTable;
	private ScrollPanel scrollPanel;
	private VerticalPanel verticalPanel;
	
	private Set<String> selectedCompounds = new HashSet<String>();
	private Column<String, Boolean> selectColumn;
	private ListDataProvider<String> provider = new ListDataProvider<String>();
	
	//compound sorter widgets
	private VerticalPanel csVerticalPanel = new VerticalPanel();
	private TextBox[] sortProbeText = new TextBox[5];
	private ListBox[] rankType = new ListBox[5];
	private TextBox[] syntheticCurveText = new TextBox[5];
	
	private Map<String, Double> scores = new HashMap<String, Double>(); //for compound ranking
	
	/**
	 * @wbp.parser.constructor
	 */
	public CompoundSelector() {
		this("Compounds");
	}
	
	private void makeCompoundSorter() {
		csVerticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Label l = new Label("Compound ranking");
		l.setStyleName("heading");
		csVerticalPanel.add(l);
		
		Grid g = new Grid(6, 3);
		csVerticalPanel.add(g);
		g.setWidget(0, 0, new Label("Protein/gene/probe"));
		g.setWidget(0, 1, new Label("Match type"));
		g.setWidget(0, 2, new Label("Synthetic curve"));
		
		for (int i = 0; i < 5; i++) {
			sortProbeText[i] = new TextBox();
			rankType[i] = new ListBox();
			rankType[i].addItem("Increasing");
			rankType[i].addItem("Decreasing");
			rankType[i].addItem("Increasing 2");
			rankType[i].addItem("Decreasing 2");
			rankType[i].addItem("Synthetic curve");
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
				for (int i = 0; i < 5; ++i) {
					if (!sortProbeText[i].getText().equals("")) {
						String probe = sortProbeText[i].getText();
						switch (rankType[i].getSelectedIndex()) {
						case 0:
							rules[i] = new RankRule.Increasing(probe);
							break;
						case 1:
							rules[i] = new RankRule.Decreasing(probe);
							break;
						case 2:
							rules[i] = new RankRule.Increasing2(probe);
							break;
						case 3:
							rules[i] = new RankRule.Decreasing2(probe);
							break;
						case 4:
							double[] data = new double[4];
							String[] ss = syntheticCurveText[i].getText()
									.split(" ");
							if (ss.length != 4) {
								Window.alert("Please supply 4 space-separated values as the synthetic curve. (Example: -1 -2 -3 -4");
							} else {
								for (int j = 0; j < ss.length; ++j) {
									data[j] = Double.valueOf(ss[j]);
								}
								rules[i] = new RankRule.Synthetic(probe, data);
							}
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
									provider.setList(sortedCompounds);
									compoundTable.setVisibleRange(0,
											sortedCompounds.size());
								}

								public void onFailure(Throwable caught) {
									Window.alert("Unable to rank compounds.");
								}
							});
				}
			}
		});
		
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
		
		compoundTable = new CellTable<String>();
		scrollPanel.setWidget(compoundTable);
		compoundTable.setSize("100%", "100%");
		compoundTable.setSelectionModel(new NoSelectionModel<String>());
		
		selectColumn = new Column<String, Boolean>(new CheckboxCell()) {
			@Override
			public Boolean getValue(String object) {

				return selectedCompounds.contains(object);				
			}
		};
		selectColumn.setFieldUpdater(new FieldUpdater<String, Boolean>() {
			@Override
			public void update(int index, String object, Boolean value) {
				if (value) {					
					selectedCompounds.add(object);
				} else {
					selectedCompounds.remove(object);
				}
				
				List<String> r = new ArrayList<String>();
				r.addAll(selectedCompounds);
				Collections.sort(r);
				changeCompounds(r);
			}
		});
		
		compoundTable.addColumn(selectColumn, "Selected");
		
		TextColumn<String> textColumn = new TextColumn<String>() {
			@Override
			public String getValue(String object) {
				return object;
			}
		};
		compoundTable.addColumn(textColumn, "Compound");
		
		textColumn = new TextColumn<String>() {
			@Override
			public String getValue(String object) {
				if (scores.containsKey(object)) {
					return Utils.formatNumber(scores.get(object));					
				} else {
					return "N/A";
				}
			}
		};
		compoundTable.addColumn(textColumn, "Score");
		
		provider.addDataDisplay(compoundTable);		
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);		
		loadCompounds();
		selectedCompounds.clear();
		
		//reset any edits the user might have done
		for (String item: provider.getList()) {
			((CheckboxCell) selectColumn.getCell()).clearViewData(provider.getKey(item));
		}
	}
	
	public List<String> getCompounds() {				
		List<String> r = new ArrayList<String>();
		r.addAll(selectedCompounds);		
		Collections.sort(r);
		return r;
	}

	void loadCompounds() {		
		owlimService.compounds(chosenDataFilter, new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				Arrays.sort(result);
				List<String> r = new ArrayList<String>((Arrays.asList(result)));				
				provider.setList(r);
				compoundTable.setVisibleRange(0, r.size());
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to retrieve compounds");			
			}
		});
	}
	
	public void setSelection(List<String> compounds) {		
		selectedCompounds.clear();
		selectedCompounds.addAll(compounds);
		
		//reset any edits the user might have done
		for (String item: provider.getList()) {
			((CheckboxCell) selectColumn.getCell()).clearViewData(provider.getKey(item));
		}
		
		compoundTable.redraw();		
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
		PopupPanel pp = new PopupPanel(true, true);
		pp.setWidget(csVerticalPanel);
		pp.setPopupPosition(Window.getClientWidth()/2 - 100, Window.getClientHeight()/2 - 100);
		pp.show();
	}
}
