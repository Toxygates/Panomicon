package otgviewer.shared;

import java.io.Serializable;

/**
 * Rule types for compound ranking. See otgviewer.server.Conversions for
 * conversions to the Scala equivalents.
 * @author johan
 *
 */
public enum RuleType implements Serializable {
	Sum("Total upregulation"),
	NegativeSum("Total downregulation"), 		
	Synthetic("User pattern"),		
	MaximalFold("Maximal fold"),		
	MinimalFold("Minimal fold"),		
	ReferenceCompound("Reference compound"),		
	MonotonicUp("Monotonic up"),		
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

 