package t.common.shared;

import java.io.Serializable;

public enum ValueType implements Serializable {
	Folds {
		public String toString() {
			return "Log2 (fold change)";
		}
	},
	Absolute {
		public String toString() {
			return "Normalized intensity";
		}
	};
	
	public static ValueType unpack(String type) {
		if (type.equals(Folds.toString())) {
			return Folds;
		} 
		return Absolute;
	}
}
