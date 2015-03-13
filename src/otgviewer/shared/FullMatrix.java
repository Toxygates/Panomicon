package otgviewer.shared;

import java.io.Serializable;
import java.util.List;

import t.common.shared.sample.ExpressionRow;

public class FullMatrix implements Serializable {

	public FullMatrix() {}
	
	public FullMatrix(ManagedMatrixInfo mmi, List<ExpressionRow> data) {
		this.mmi = mmi;
		this.data = data;
	}
	
	private ManagedMatrixInfo mmi;
	public ManagedMatrixInfo managedMatrixInfo() { return mmi; }
	
	private List<ExpressionRow> data;
	public List<ExpressionRow> rows() { return data; }

}
