package otgviewer.shared;

import java.io.Serializable;

public enum CellType implements Serializable {
	Vivo { 
		public String toString() {
			return "In Vivo";
		}
	},
	Vitro {
		public String toString() {
			return "In Vitro";
		}
	}
}
