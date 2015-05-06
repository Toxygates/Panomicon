package otgviewer.shared;

import java.io.Serializable;

import t.common.shared.HasClass;
import t.common.shared.SampleClass;
import t.common.shared.sample.ExpressionValue;

/**
 * An expression value series that fixes all parameters except one, which
 * varies on the x-axis.
 */
public class Series implements HasClass, Serializable {

	public Series() {
		
	}
	
	/**
	 * Construct a new series.
	 * @param title Description of this series
	 * @param probe
	 * @param independentParam The parameter that is varied on the x-axis. For example, in OTG,
	 * if exposure_time is independent, then this is a time series.
	 * @param values Data values for this series (ordered by dose if the time is fixed, 
	 * or ordered by time if the dose is fixed)
	 * @param sc Sample class parameters
	 * @param values Data points
	 */
	public Series(String title, String probe, String independentParam, 
			SampleClass sc, ExpressionValue[] values) {
		_values = values;
		_title = title;
		_probe = probe;		
		_independentParam = independentParam;
		_sc = sc;
	}
	
	private SampleClass _sc;
	public SampleClass sampleClass() { return _sc; }
	
	private String _probe;
	public String probe() { return _probe; }
	
	private String _independentParam;
	public String independentParam() { return _independentParam; }
	
	//TODO users should access the sample class instead
	@Deprecated
	public String timeDose() {
		//TODO don't hardcode this
		String fixedParam =
				_independentParam.equals("exposure_time") ? "dose_level" : "exposure_time";
		return _sc.get(fixedParam); 
	}
	
	//TODO users should access the sample class instead
	@Deprecated
	public String compound() { return _sc.get("compound_name"); }
	
	private ExpressionValue[] _values;	
	public ExpressionValue[] values() { return _values; }
	
	private String _title;
	public String title() { return _title; }
	
	private String _organism;
	
	//TODO users should access the sample class instead
	@Deprecated
	public String organism() { return _sc.get("organism"); }
	
	public String get(String key) {
		return _sc.get(key);
	}
}
