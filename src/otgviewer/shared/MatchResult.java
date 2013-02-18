package otgviewer.shared;

import java.io.Serializable;

public class MatchResult implements Serializable {

	public MatchResult() { }		
	
	private int _dose;
	public int dose() { return _dose; }
	
	private double _score;
	public double score() { return _score; }
	
	private String _compound;
	public String compound() { return _compound; }
	
	public MatchResult(String compound, double score, int dose) {
		_dose = dose;
		_compound = compound;
		_score = score;
	}
}
