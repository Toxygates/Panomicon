package otgviewer.shared;

import java.io.Serializable;
import java.util.Arrays;

public class ExpressionRow implements Comparable<ExpressionRow>, Serializable {
	private String probe = "";
	private String title = "";
	private String geneId = "";
	private ExpressionValue[] val = new ExpressionValue[0];;	
	
	public ExpressionRow() { }		
	
	public ExpressionRow(String _probe, String _title, String _geneId, ExpressionValue[] _val) {
		probe = _probe;
		val = _val;
		title = _title;		
		geneId = _geneId;
	}
		
	public ExpressionRow(String _probe, String _title, String _geneId, ExpressionValue _val) {
		probe = _probe;
		val = new ExpressionValue[] { _val };
		title = _title;		
		geneId = _geneId;
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
	
	public ExpressionValue getValue(int i) {		
		if (i < val.length) {
			return val[i];
		} else {
			return new ExpressionValue(0, 'A');
		}
	}
	
	public String getTitle() {
		return title;
	}	
	
	public String getGeneId() {
		return geneId;
	}
	
	public int compareTo(ExpressionRow o) {		
		if (o == null) {
			return -1;
		} else {
			return probe.compareTo(o.probe);			
		}			
	}
	
}
