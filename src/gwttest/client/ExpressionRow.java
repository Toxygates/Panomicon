package gwttest.client;

import java.io.Serializable;

public class ExpressionRow implements Comparable<ExpressionRow>, Serializable {
	private String probe;
	private String title;
	private Double val;	
	
	public ExpressionRow() {
		probe = "";
		val = 0.0;
		title = "";
	}
	
	public ExpressionRow(String _probe, String _title, Double _val) {
		probe = _probe;
		val = _val;
		title = _title;
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
	public String getTitle() {
		return title;
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
