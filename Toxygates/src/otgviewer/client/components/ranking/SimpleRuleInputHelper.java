package otgviewer.client.components.ranking;

import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;

public class SimpleRuleInputHelper extends RuleInputHelper {

	public SimpleRuleInputHelper(CompoundRanker _ranker, RankRule r,
			boolean lastRule) {
		super(_ranker, r, lastRule);
	}	
	
	final static int REQUIRED_COLUMNS = 3;

	protected RuleType[] ruleTypes() {
		return new RuleType[] { RuleType.Sum, RuleType.NegativeSum };
	}

}
