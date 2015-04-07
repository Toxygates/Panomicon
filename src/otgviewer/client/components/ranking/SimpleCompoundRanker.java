package otgviewer.client.components.ranking;

import java.util.List;

import otgviewer.client.CompoundSelector;
import otgviewer.client.Utils;
import otgviewer.client.components.Screen;
import otgviewer.shared.RankRule;

public class SimpleCompoundRanker extends CompoundRanker {

	public SimpleCompoundRanker(Screen _screen, CompoundSelector selector) {
		super(_screen, selector);		
	}

	protected void addHeaderWidgets() {
		grid.setWidget(0, 1, Utils.mkEmphLabel("Gene/probe"));
		grid.setWidget(0, 2, Utils.mkEmphLabel("Match type"));				
	}
	
	protected int gridColumns() {
		return SimpleRuleInputHelper.REQUIRED_COLUMNS;
	}
	
	protected RuleInputHelper makeInputHelper(RankRule r, boolean isLast) {
		return new SimpleRuleInputHelper(this, r, isLast);
	}

}
