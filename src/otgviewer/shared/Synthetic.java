package otgviewer.shared;

import java.io.Serializable;

import t.common.shared.sample.DataColumn;

abstract public class Synthetic implements DataColumn<OTGSample>, Serializable {

	public abstract static class TwoGroupSynthetic extends Synthetic {
		protected Group g1, g2;
		public Group getGroup1() { return g1; }
		public Group getGroup2() { return g2; }
		
		public TwoGroupSynthetic(String title, Group g1, Group g2) {		
			super(title);
			setGroups(g1, g2);			
		}
		
		public void setGroups(Group g1, Group g2) {
			this.g1 = g1;
			this.g2 = g2;
		}
	}
	
	/**
	 * Student's T-Test.
	 * The test is two-tailed and does not assume equal sample variances.
	 */
	public static class TTest extends TwoGroupSynthetic {		
		public TTest() { super("", null, null); }			
		public TTest(Group g1, Group g2) {			
			super("", g1, g2);
		}	
		
		@Override
		public void setGroups(Group g1, Group g2) {
			super.setGroups(g1, g2);
			if (g1 != null && g2 != null) {
				name = "(T) p(" + g1.getShortTitle() + ", " + g2.getShortTitle() + ")";
			}
		}
		
		@Override
		public String getTooltip() {
			return "p-value";
		}
	}
	
	/**
	 * Mann-Whitney U-test.
	 */
	public static class UTest extends TwoGroupSynthetic {		
		public UTest() { super("", null, null); }			
		public UTest(Group g1, Group g2) {
			super("", g1, g2);
		}	
		
		@Override
		public void setGroups(Group g1, Group g2) {
			super.setGroups(g1, g2);
			if (g1 != null && g2 != null) {
				name = "(U) p(" + g1.getShortTitle() + ", " + g2.getShortTitle() + ")";
			}
		}
		@Override
		public String getTooltip() {
			return "p-value (nonparametric)";
		}
	}
	
	/**
	 * Mean difference between groups.
	 */
	public static class MeanDifference extends TwoGroupSynthetic {
		public MeanDifference() { super("", null, null); }
		public MeanDifference(Group g1, Group g2) {
			super("", g1, g2);
		}
		
		@Override
		public void setGroups(Group g1, Group g2) {
			super.setGroups(g1, g2);
			if (g1 != null && g2 != null) {
				name = "Diff(" + g1.getShortTitle() + ", " + g2.getShortTitle() + ")";
			}
		}
		@Override
		public String getTooltip() {
			return "Fold change difference";
		}
		
		@Override
		public boolean isDefaultSortAscending() { 
			return false; 
		}
	}

	protected String name;
	
	public Synthetic() { }		
	public Synthetic(String name) { this.name = name; }
	public OTGSample[] getSamples() { return new OTGSample[0]; }	
	public String[] getCompounds() { return new String[0]; }	
	public String getShortTitle() { return name; }	
	public String getTooltip() { return "Synthetic"; }
	public boolean isDefaultSortAscending() { return true; }
	
	public String pack() {
		return "Synthetic:::" + name;
	}
}
