package otgviewer.shared;

import java.io.Serializable;

public enum RuleType implements Serializable {			
	Sum, NegativeSum,
	Synthetic,
	MonotonicUp, MonotonicDown,
	Unchanged,
	LowVariance, HighVariance
}

