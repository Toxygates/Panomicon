package otgviewer.shared;

import java.io.Serializable;

/**
 * Ranking rules for compounds.
 * @author johan
 *
 */
public class RankRule implements Serializable {
	
	public RankRule() { }
	public RankRule(RuleType type, String probe) {
		_probe = probe;
		_type = type;
	}
	
	private RuleType _type;
	public RuleType type() { return _type; }
	
	private String _probe;
	public String probe() { return _probe; }
		
	private double[] _data;
	public double[] data() {
		return _data;
	}
	
	public void setData(double[] data) {
		_data = data;
	}

}
