package otgviewer.shared;

import java.io.Serializable;

/**
 * Ranking rules for compounds. Also see RuleType.
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
		
	//Used by reference compound rule only
	private String _compound;
	public String compound() { return _compound; }	
	public void setCompound(String compound) { _compound = compound; }
	
	//Used by synthetic rule only
	private double[] _data;
	public double[] data() { return _data; }
	public void setData(double[] data) { _data = data; }
	
	private String _dose;
	public String dose() { return _dose; }	
	public void setDose(String dose) { _dose = dose; }
	
}
