package otgviewer.shared;

import java.io.Serializable;

/**
 * Ranking rule for compounds.
 * @author johan
 *
 */
public abstract class RankRule implements Serializable {

	public RankRule() { }
	public RankRule(String probe) {
		_probe = probe;
	}
	
	private String _probe;
	public String probe() { return _probe; }
	
	public static class Increasing extends RankRule {
		public Increasing() {}
		public Increasing(String probe) {
			super(probe);
		}
	}
	
	public static class Decreasing extends  RankRule {
		public Decreasing() {}
		public Decreasing(String probe) {
			super(probe);
		}
	}
	
	public static class Synthetic extends RankRule {
		public Synthetic() {}
		public Synthetic(String probe, double[] data) {
			super(probe);
			_data = data;
		}
		
		private double[] _data;
		public double[] data() { return _data; }
	}
}
