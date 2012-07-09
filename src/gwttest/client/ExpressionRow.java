package gwttest.client;

import java.io.Serializable;

public class ExpressionRow implements Comparable<ExpressionRow>, Serializable {
	private String probe;
	private Double val;
	
	public ExpressionRow() {
		probe = "";
		val = 0.0;
	}
	
	public ExpressionRow(String _probe, Double _val) {
		probe = _probe;
		val = _val;
	}
	
	public boolean equals(Object o) {
		if (o instanceof ExpressionRow) {
			return (probe == ((ExpressionRow) o).probe && val == ((ExpressionRow)o).val);
		}
		return false;
	}
	
	public String getProbe() {
		return probe;
	}
	public Double getValue() {
		return val;
	}
	
	public int compareTo(ExpressionRow o) {		
		if (o == null) {
			return -1;
		} else {
			int pc = probe.compareTo(o.probe);
			if (pc != 0) {
				return pc;
			} else {
				return val.compareTo(o.val);
			}	
		}			
	}
	
}
