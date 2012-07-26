package otgviewer.shared;

import java.io.Serializable;

public enum CellType implements Serializable {
	Vivo { 
		public String toString() {
			return "in vivo";
		}
	},
	Vitro {
		public String toString() {
			return "in vitro";
		}
	}
}
