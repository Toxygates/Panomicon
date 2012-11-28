package otgviewer.shared;

import java.io.Serializable;

public enum RuleType implements Serializable {			
	Sum, NegativeSum,
	Synthetic,
	ReferenceCompound,
	MonotonicUp, MonotonicDown,
	Unchanged,
	LowVariance, HighVariance	
}

