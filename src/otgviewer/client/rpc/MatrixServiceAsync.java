package otgviewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.ManagedMatrixInfo;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.viewer.shared.StringList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MatrixServiceAsync {

	public void identifiersToProbes(String[] identifiers,
			boolean precise, AsyncCallback<String[]> callback);

	public void loadDataset(List<Group> columns,
			String[] probes, ValueType type, List<Synthetic> synthCols, 
			AsyncCallback<ManagedMatrixInfo> callback);

	public void selectProbes(String[] probes,
			AsyncCallback<ManagedMatrixInfo> callback);

	public void setColumnThreshold(int column, @Nullable Double threshold,
			AsyncCallback<ManagedMatrixInfo> callback);
	
	public void datasetItems(int offset, int size, int sortColumn,
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);

	public void getFullData(Group g, String[] probes,
			boolean sparseRead, boolean withSymbols, ValueType typ,
			AsyncCallback<List<ExpressionRow>> callback);

	public void prepareCSVDownload(AsyncCallback<String> callback);

	public void getGenes(int limit, AsyncCallback<String[]> callback);
	
	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test,
			AsyncCallback<Void> callback);
	
	public void removeTwoGroupTests(AsyncCallback<Void> callback);
	
	public void importTargetmineLists(String user, String pass, 
			boolean asProbes, AsyncCallback<StringList[]> callback);
	
	public void exportTargetmineLists(String user, String pass, 
			StringList[] lists, boolean replace, AsyncCallback<Void> callback);
	
	public void sendFeedback(String name, String email, String feedback, 
			AsyncCallback<Void> callback);
	
}
