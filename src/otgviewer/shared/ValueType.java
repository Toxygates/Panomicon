package otgviewer.shared;

import java.io.Serializable;

public enum ValueType implements Serializable {
	Folds {
		public String toString() {
			return "Log2 (fold change)";
		}
	},
	Absolute;
	
	public static ValueType unpack(String type) {
		if (type.equals(Folds.toString())) {
			return Folds;
		} 
		return Absolute;
	}
}
