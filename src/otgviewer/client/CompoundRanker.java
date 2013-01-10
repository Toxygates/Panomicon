package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.shared.DataFilter;
import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CompoundRanker extends DataListenerWidget {
	private static Resources resources = GWT.create(Resources.class);
	private CompoundSelector selector;

	private VerticalPanel csVerticalPanel = new VerticalPanel();
	private final int RANK_CONDS = 10;
	private SuggestBox[] rankProbeText = new SuggestBox[RANK_CONDS];
	private ListBox[] rankType = new ListBox[RANK_CONDS];
	private ListBox[] rankRefCompound = new ListBox[RANK_CONDS];
	private ListBox[] rankRefDose = new ListBox[RANK_CONDS];
	private TextBox[] syntheticCurveText = new TextBox[RANK_CONDS];
	private CheckBox[] rankCheckBox = new CheckBox[RANK_CONDS];
	private List<String> rankProbes = new ArrayList<String>();

	public CompoundRanker(CompoundSelector selector) {
		this.selector = selector;
		selector.addListener(this);

		csVerticalPanel
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
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

		HorizontalPanel hp = Utils.mkHorizontalPanel();
		csVerticalPanel.add(hp);
		
		hp.add(new Button("Rank", new ClickHandler() {
			public void onClick(ClickEvent event) {
				performRanking();
			}
		}));
		
		
		Widget i = Utils.mkHelpButton(resources.compoundRankingHTML(), resources.compoundRankingHelp());
		hp.add(i);
	}

	final GeneOracle oracle = new GeneOracle();
	private void makeRankRuleInputs(Grid g, final int row) {
		
		rankProbeText[row] = new SuggestBox(oracle);
		
		rankType[row] = new ListBox();
		for (RuleType rt : RuleType.values()) {
			rankType[row].addItem(rt.toString());
		}
		rankType[row].addChangeHandler(rankTypeChangeHandler(row));
		rankCheckBox[row] = new CheckBox();
		
		rankProbeText[row].addKeyPressHandler(new KeyPressHandler() {			
			@Override
			public void onKeyPress(KeyPressEvent event) {
				rankCheckBox[row].setValue(true);				
			}
		});
		
		syntheticCurveText[row] = new TextBox();
		syntheticCurveText[row].setWidth("5em");
		syntheticCurveText[row].setEnabled(false);
		
		rankRefCompound[row] = new ListBox();
		rankRefCompound[row].setStyleName("colored");
		rankRefCompound[row].setEnabled(false);
		
		ListBox lb = new ListBox();
		rankRefDose[row] = lb;		
		lb.setStyleName("colored");
		lb.addItem("Low"); //TODO! read proper doses from db
		lb.addItem("Middle");
		lb.addItem("High");
		lb.setEnabled(false);

		g.setWidget(row + 1, 0, rankCheckBox[row]);
		g.setWidget(row + 1, 1, rankProbeText[row]);
		g.setWidget(row + 1, 2, rankType[row]);
		g.setWidget(row + 1, 3, syntheticCurveText[row]);
		g.setWidget(row + 1, 4, rankRefCompound[row]);
		g.setWidget(row + 1, 5, rankRefDose[row]);
	}

	private ChangeHandler rankTypeChangeHandler(final int row) {
		return new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				RuleType rt = selectedRuleType(row);
				switch (rt) {
				case Synthetic:
					syntheticCurveText[row].setEnabled(true);
					rankRefCompound[row].setEnabled(false);
					rankRefDose[row].setEnabled(false);
					break;
				case ReferenceCompound:
					syntheticCurveText[row].setEnabled(false);
					rankRefCompound[row].setEnabled(true);
					rankRefDose[row].setEnabled(true);
					break;
				default:
					syntheticCurveText[row].setEnabled(false);
					rankRefCompound[row].setEnabled(false);
					rankRefDose[row].setEnabled(false);
					break;
				}
			}
		};
	}

	private RuleType selectedRuleType(int row) {
		return RuleType.parse(rankType[row].getItemText(rankType[row]
				.getSelectedIndex()));		
	}

	private void performRanking() {
		List<RankRule> rules = new ArrayList<RankRule>();
		rankProbes = new ArrayList<String>();
		for (int i = 0; i < RANK_CONDS; ++i) {
			if (rankCheckBox[i].getValue()) {
				if (!rankProbeText[i].getText().equals("")) {
					String probe = rankProbeText[i].getText();
					rankProbes.add(probe);
					RuleType rt = selectedRuleType(i);
					switch (rt) {
					case Synthetic: {
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
					}
						break;
					case ReferenceCompound: {
						String cmp = rankRefCompound[i]
								.getItemText(rankRefCompound[i]
										.getSelectedIndex());
						RankRule r = new RankRule(rt, probe);
						r.setCompound(cmp);
						r.setDose(rankRefDose[i].getItemText(rankRefDose[i]
								.getSelectedIndex()));
						rules.add(r);
					}
						break;
					default:
						// rule is not synthetic or ref compound
						rules.add(new RankRule(rt, probe));
						break;
					}
				} else {
					// gene name is not empty
					Window.alert("Empty gene name detected. Please specify a gene/probe for each enabled rule.");
				}
			}
		}		
		selector.performRanking(rankProbes, rules);
	}

	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);
		for (ListBox lb : rankRefCompound) {
			lb.clear();
		}
	}

	@Override
	public void availableCompoundsChanged(List<String> compounds) {
		super.compoundsChanged(compounds);
		for (ListBox lb : rankRefCompound) {
			for (String c : compounds) {
				lb.addItem(c);
			}
		}
	}
}
