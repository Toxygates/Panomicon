package otgviewer.shared;

import java.io.Serializable;

public enum RuleType implements Serializable {
	Increasing, Decreasing, 	
	Synthetic, LowVariance, HighVariance,
	MonotonicUp, MonotonicDown,	
	Unchanged,
	Sum, NegativeSum
}

