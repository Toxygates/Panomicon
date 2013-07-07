package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.EnumSelector;
import otgviewer.shared.CellType;
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

/**
 * This widget is an UI for defining compound ranking rules.
 * The actual ranking is requested by a compound selector and performed
 * (obviously) on the server side.
 * @author johan
 *
 */
public class CompoundRanker extends DataListenerWidget {
	private static Resources resources = GWT.create(Resources.class);
	private CompoundSelector selector;
	
	final GeneOracle oracle = new GeneOracle();
	private List<String> availableCompounds = chosenCompounds;
	
	/**
	 * Data and widgets that help the user input a rule but do not need to be
	 * sent to the server when the ranking is performed.
	 * @author johan
	 *
	 */
	private class RuleInputHelper {
		private boolean isFinalRule;
		
		RuleInputHelper(RankRule r, boolean finalRule) {
			rule = r;
			this.isFinalRule = finalRule;
			
			rankType.listBox().addChangeHandler(rankTypeChangeHandler());			
			probeText.addKeyPressHandler(new KeyPressHandler() {			
				@Override
				public void onKeyPress(KeyPressEvent event) {
					enabled.setValue(true);	
					if (isFinalRule) {
						addRule(true);
						isFinalRule = false;
					}
				}
			});
			
			syntheticCurveText.setWidth("5em");
			syntheticCurveText.setEnabled(false);
			
			refCompound.setStyleName("colored");
			refCompound.setEnabled(false);
			for (String c : availableCompounds) {
				refCompound.addItem(c);
			}
					
			refDose.setStyleName("colored");
			refDose.addItem("Low"); //TODO! read proper doses from db
			refDose.addItem("Middle");
			refDose.addItem("High");
			refDose.setEnabled(false);
		}
		
		final RankRule rule;		
		final ListBox refCompound = new ListBox();
		final ListBox refDose = new ListBox();
		final SuggestBox probeText = new SuggestBox(oracle);
		final TextBox syntheticCurveText = new TextBox();
		final CheckBox enabled = new CheckBox();
		final EnumSelector<RuleType> rankType = new EnumSelector<RuleType>() {
			protected RuleType[] values() { return RuleType.values(); }
		};
		
		void populate(int row) {
			grid.setWidget(row + 1, 0, enabled);
			grid.setWidget(row + 1, 1, probeText);
			grid.setWidget(row + 1, 2, rankType);
			grid.setWidget(row + 1, 3, syntheticCurveText);
			grid.setWidget(row + 1, 4, refCompound);
			grid.setWidget(row + 1, 5, refDose);
		}

		ChangeHandler rankTypeChangeHandler() {
			return new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					RuleType rt = selectedRuleType();
					switch (rt) {
					case Synthetic:
						syntheticCurveText.setEnabled(true);
						refCompound.setEnabled(false);
						refDose.setEnabled(false);
						break;
					case ReferenceCompound:
						syntheticCurveText.setEnabled(false);
						refCompound.setEnabled(true);
						refDose.setEnabled(true);
						break;
					default:
						syntheticCurveText.setEnabled(false);
						refCompound.setEnabled(false);
						refDose.setEnabled(false);
						break;
					}
				}
			};
		}
		
		RuleType selectedRuleType() {
			return rankType.value();		
		}
	}

	private VerticalPanel csVerticalPanel = new VerticalPanel();
	private List<String> rankProbes = new ArrayList<String>();
	
	private List<RuleInputHelper> inputHelpers = new ArrayList<RuleInputHelper>();

	private Grid grid;
	
	/**
	 * 
	 * @param selector the selector that this CompoundRanker will communicate with.
	 */
	public CompoundRanker(CompoundSelector selector) {
		this.selector = selector;
		selector.addListener(this);

		csVerticalPanel
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(csVerticalPanel);

		grid = new Grid(1, 6); //Initially space for 1 rule
		csVerticalPanel.add(grid);
		grid.setWidget(0, 1, Utils.mkEmphLabel("Gene/probe"));
		grid.setWidget(0, 2, Utils.mkEmphLabel("Match type"));
		grid.setWidget(0, 3, Utils.mkEmphLabel("User ptn."));
		grid.setWidget(0, 4, Utils.mkEmphLabel("Ref. compound"));
		grid.setWidget(0, 5, Utils.mkEmphLabel("Ref. dose"));
		
		addRule(true);
		
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

	private void addRule(boolean isFinal) {
		int ruleIdx = inputHelpers.size();
		grid.resize(ruleIdx + 2, 6);
		RankRule r = new RankRule();		
		RuleInputHelper rih = new RuleInputHelper(r, isFinal);		
		inputHelpers.add(rih);	
		rih.populate(ruleIdx);
	}
	
	private void performRanking() {
		List<RankRule> rules = new ArrayList<RankRule>();
		rankProbes = new ArrayList<String>();		
		for (RuleInputHelper rih: inputHelpers) {
			if (rih.enabled.getValue()) {
				if (!rih.probeText.getText().equals("")) {
					String probe = rih.probeText.getText();
					rankProbes.add(probe);
					RuleType rt = rih.selectedRuleType();
					switch (rt) {
					case Synthetic: {
						double[] data;
						String[] ss = rih.syntheticCurveText.getText()
								.split(" ");
						RankRule r = new RankRule(rt, probe);
						if (ss.length != 4 && chosenDataFilter.cellType == CellType.Vivo) {
							Window.alert("Please supply 4 space-separated values as the synthetic curve. (Example: -1 -2 -3 -4)");
						} else if (ss.length != 3 && chosenDataFilter.cellType == CellType.Vitro) {
							Window.alert("Please supply 3 space-separated values as the synthetic curve. (Example: -1 -2 -3)");
						} else {
							if (chosenDataFilter.cellType == CellType.Vivo) {
								data = new double[4];
							} else {
								data = new double[3];
							}
							for (int j = 0; j < ss.length; ++j) {
								data[j] = Double.valueOf(ss[j]);
							}
							r.setData(data);
							rules.add(r);
						}
					}
						break;
					case ReferenceCompound: {
						String cmp = rih.refCompound
								.getItemText(rih.refCompound
										.getSelectedIndex());
						RankRule r = new RankRule(rt, probe);
						r.setCompound(cmp);
						r.setDose(rih.refDose.getItemText(rih.refDose
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
		oracle.setFilter(filter);	
		for (RuleInputHelper rih: inputHelpers) {
			rih.refCompound.clear();
		}		
	}

	@Override
	public void availableCompoundsChanged(List<String> compounds) {
		super.availableCompoundsChanged(compounds);
		availableCompounds = compounds;
		for (RuleInputHelper rih: inputHelpers) {
			rih.refCompound.clear();
			for (String c : compounds) {
				rih.refCompound.addItem(c);
			}
		}
	}
}
