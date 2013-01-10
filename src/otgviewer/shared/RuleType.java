package otgviewer.shared;

import java.io.Serializable;

public enum RuleType implements Serializable {
	Sum("Total upregulation"),
	NegativeSum("Total downregulation"), 		
	Synthetic("User pattern"),		
	MaximalFold("Maximal fold"),		
	MinimalFold("Minimal fold"),		
	ReferenceCompound("Reference compound"),		
	MonotonicUp("Montonic up"),		
	MonotonicDown("Monotonic down"),
	Unchanged("Unchanged"),
	LowVariance("Low variance"),
	HighVariance("High variance");
	
	private String name;
	private RuleType(String name) {
		this.name = name;
	}
	public String toString() {
		return name;
	}
	
	public static RuleType parse(String s) {
		for (RuleType rt: RuleType.values()) {
			if (s.equals(rt.toString())) {
				return rt;
			}
		}
		return null;
	}
}

 