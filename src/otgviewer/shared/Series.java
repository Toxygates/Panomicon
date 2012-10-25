package otgviewer.shared;

import java.io.Serializable;

public class Series implements Serializable {

	public Series() {
		
	}
	
	/**
	 * Construct a new series.
	 * @param title Description of this series
	 * @param timeDose The fixed time OR fixed dose that this series corresponds to.
	 * @param compound
	 * @param probe
	 * @param values Data values for this series (ordered by dose if the time is fixed, 
	 * or ordered by time if the dose is fixed)
	 */
	public Series(String title, String probe, String timeDose, String compound, ExpressionValue[] values) {
		_values = values;
		_title = title;
		_probe = probe;
		_timeDose = timeDose;
		_compound = compound;
	}
	
	private String _probe;
	public String probe() {
		return _probe;
	}
	
	private String _timeDose;
	public String timeDose() {
		return _timeDose;
	}
	
	private String _compound;
	public String compound() {
		return _compound;
	}
	
	private ExpressionValue[] _values;	
	public ExpressionValue[] values() {
		return _values;
	}
	
	private String _title;
	public String title() {
		return _title;
	}
}
