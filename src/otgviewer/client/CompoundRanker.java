package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.shared.DataFilter;
import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CompoundRanker extends DataListenerWidget {

	private CompoundSelector selector;

	private VerticalPanel csVerticalPanel = new VerticalPanel();
	private final int RANK_CONDS = 10;
	private TextBox[] sortProbeText = new TextBox[RANK_CONDS];
	private ListBox[] rankType = new ListBox[RANK_CONDS];
	private ListBox[] rankRefCompound = new ListBox[RANK_CONDS];
	private ListBox[] rankRefDose = new ListBox[RANK_CONDS];
	private TextBox[] syntheticCurveText = new TextBox[RANK_CONDS];
	private CheckBox[] rankCheckBox = new CheckBox[RANK_CONDS];
	private List<String> rankProbes = new ArrayList<String>();

	public CompoundRanker(CompoundSelector selector) {
		this.selector = selector;
		selector.addListener(this);
		
		csVerticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(csVerticalPanel);

		Grid g = new Grid(RANK_CONDS + 1, 6);		
		csVerticalPanel.add(g);
		g.setWidget(0, 1, Utils.mkEmphLabel("Gene/probe"));
		g.setWidget(0, 2, Utils.mkEmphLabel("Match type"));
		g.setWidget(0, 3, Utils.mkEmphLabel("Synth. curve"));
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

	private void makeRankRuleInputs(Grid g, int row) {
		sortProbeText[row] = new TextBox();
		rankType[row] = new ListBox();
		for (RuleType rt: RuleType.values()) {
			rankType[row].addItem(rt.toString());
		}
		rankCheckBox[row] = new CheckBox();
		syntheticCurveText[row] = new TextBox();
		syntheticCurveText[row].setWidth("5em");
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
								.getItemText(rankRefCompound[i]
										.getSelectedIndex());
						RankRule r = new RankRule(rt, probe);
						r.setCompound(cmp);
						r.setDose(rankRefDose[i].getItemText(rankRefDose[i]
								.getSelectedIndex()));
						rules.add(r);

					} else {
						// rule is not synthetic or ref compound
						rules.add(new RankRule(rt, probe));
					}
				} else {
					// gene name is not empty
					Window.alert("Empty gene name detected. Please specify a gene/probe for each enabled rule.");
				}
			}
		}
		selector.setRankProbes(rankProbes);
		selector.performRanking(rules);
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {		
		super.dataFilterChanged(filter);
		for (ListBox lb: rankRefCompound) {
			lb.clear();
		}
	}

	@Override
	public void availableCompoundsChanged(List<String> compounds) {		
		super.compoundsChanged(compounds);
		for (ListBox lb: rankRefCompound) {
			for (String c: compounds) {
				lb.addItem(c);
			}
		}
	}
}
