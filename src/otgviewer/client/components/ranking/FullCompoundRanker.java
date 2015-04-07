package otgviewer.client.components.ranking;

import java.util.List;

import otgviewer.client.CompoundSelector;
import otgviewer.client.Utils;
import otgviewer.client.components.Screen;
import otgviewer.shared.RankRule;

public class FullCompoundRanker extends CompoundRanker {

	public FullCompoundRanker(Screen _screen, CompoundSelector selector) {
		super(_screen, selector);		
	}
	
	protected void addHeaderWidgets() {
		grid.setWidget(0, 1, Utils.mkEmphLabel("Gene/probe"));
		grid.setWidget(0, 2, Utils.mkEmphLabel("Match type"));
		grid.setWidget(0, 3, Utils.mkEmphLabel("User ptn."));
		grid.setWidget(0, 4, Utils.mkEmphLabel("Ref. compound"));
		grid.setWidget(0, 5, Utils.mkEmphLabel("Ref. dose"));		
	}
	
	protected int gridColumns() {
		return FullRuleInputHelper.REQUIRED_COLUMNS;
	}
	
	protected RuleInputHelper makeInputHelper(RankRule r, boolean isLast) {
		return new FullRuleInputHelper(this, r, isLast);
	}
}
