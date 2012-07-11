package gwttest.client;

import java.io.Serializable;
import java.util.Arrays;

public class ExpressionRow implements Comparable<ExpressionRow>, Serializable {
	private String probe;
	private String title;
	private Double[] val;	
	
	public ExpressionRow() {
		probe = "";
		val = new Double[0];
		title = "";
	}
	
	public ExpressionRow(String _probe, String _title, Double[] _val) {
		probe = _probe;
		val = _val;
		title = _title;
	}
	
	public ExpressionRow(String _probe, String _title, Double _val) {
		probe = _probe;
		val = new Double[] { _val };
		title = _title;
	}
	
	public boolean equals(Object o) {
		if (o instanceof ExpressionRow) {
			return (probe == ((ExpressionRow) o).probe && Arrays.equals(val, ((ExpressionRow)o).val));
		}
		return false;
	}
	
	public String getProbe() {
		return probe;
	}
	
	public Double getValue(int i) {
		if (i < val.length) {
			return val[i];
		} else {
			return 0d;
		}
	}
	public String getTitle() {
		return title;
	}
	
	public int compareTo(ExpressionRow o) {		
		if (o == null) {
			return -1;
		} else {
			return probe.compareTo(o.probe);			
		}			
	}
	
}
