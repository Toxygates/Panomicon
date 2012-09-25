package otgviewer.shared;

import java.io.Serializable;

abstract public class Synthetic implements DataColumn, Serializable {

	public static class TTest extends Synthetic {
		private Group g1, g2;
		
		public TTest() { }
			
		public TTest(Group g1, Group g2) {
			super("T(" + g1.getShortTitle() + ", " + g2.getShortTitle() + ")");
			this.g1 = g1;
			this.g2 = g2;
		}
		
		public Group getGroup1() { return g1; }
		public Group getGroup2() { return g2; }
		
	}
	
	private static final long serialVersionUID = 1990541110612881170L;
	
	private String name;
	
	public Synthetic() { }
		
	public Synthetic(String name) { this.name = name; }

	public Barcode[] getBarcodes() { return new Barcode[0]; }
	
	public String[] getCompounds() { return new String[0]; }
	
	public String getShortTitle() { return name; }
	
	public String pack() {
		return "Synthetic:::" + name;
	}
}
