package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.shared.FullMatrix;
import otgviewer.shared.Group;
import otgviewer.shared.ManagedMatrixInfo;
import otgviewer.shared.Synthetic;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MatrixServiceAsync {
	
	public void loadMatrix(List<Group> columns,
			String[] probes, ValueType type, List<Synthetic> synthCols, 
			AsyncCallback<ManagedMatrixInfo> callback);

	public void matrixRows(int offset, int size, int sortColumn,
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);
	
	public void identifiersToProbes(String[] identifiers,
			boolean precise, boolean titlePatternMatch,
			AsyncCallback<String[]> callback);

	public void selectProbes(String[] probes,
			AsyncCallback<ManagedMatrixInfo> callback);

	public void setColumnThreshold(int column, @Nullable Double threshold,
			AsyncCallback<ManagedMatrixInfo> callback);

	public void getFullData(List<Group> g, String[] probes,
			boolean sparseRead, boolean withSymbols, ValueType typ,
			AsyncCallback<FullMatrix> callback);

	public void prepareCSVDownload(boolean individualSamples,
			AsyncCallback<String> callback);

	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test,
			AsyncCallback<Void> callback);
	
	public void removeTwoGroupTests(AsyncCallback<Void> callback);

	public void sendFeedback(String name, String email, String feedback, 
			AsyncCallback<Void> callback);
	
}
