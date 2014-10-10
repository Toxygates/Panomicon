package t.common.shared.sample;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Expression data for a particular set of columns for a single probe.
 * May also contain associated information such as gene IDs and gene symbols.
 * 
 * TODO this class is due for an overhaul.
 */
public class ExpressionRow implements Comparable<ExpressionRow>, Serializable {
	private String probe = "";
	private String title = "";
	private String[] geneIds = new String[0];
	private String[] geneSyms = new String[0];
	private ExpressionValue[] val = new ExpressionValue[0];	
	
	public ExpressionRow() { }		
	
	public ExpressionRow(String _probe, String _title, String[] _geneId, String[] _geneSym, ExpressionValue[] _val) {
		probe = _probe;
		val = _val;
		title = _title;		
		geneIds = _geneId;
		geneSyms = _geneSym;		
	}
	
	public boolean equals(Object o) {
		if (o instanceof ExpressionRow) {
			return (probe == ((ExpressionRow) o).probe && 
					Arrays.equals(val, ((ExpressionRow)o).val));
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
			return emptyValue();
		}
	}
	
	private ExpressionValue emptyValue() {
		return new ExpressionValue(0, 'A');
	}
	
	public ExpressionValue[] getValues() {
		return val;
	}
	
	/**
	 * Obtain the number of data columns contained.
	 * @return
	 */
	public int getColumns() {
		return val.length;
	}
	
	public String getTitle() {
		return title;
	}	
	
	public String[] getGeneIds() {
		return geneIds;
	}
	
	public String[] getGeneSyms() {
		return geneSyms;
	}

	public int compareTo(ExpressionRow o) {		
		if (o == null) {
			return -1;
		} else {
			return probe.compareTo(o.probe);			
		}			
	}	
}
