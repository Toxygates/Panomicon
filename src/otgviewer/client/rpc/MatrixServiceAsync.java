package otgviewer.client.rpc;

import java.util.List;

import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;

import bioweb.shared.array.ExpressionRow;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MatrixServiceAsync {

	public void identifiersToProbes(DataFilter filter, String[] identifiers,
			boolean precise, AsyncCallback<String[]> callback);

	public void loadDataset(DataFilter filter, List<BarcodeColumn> columns,
			String[] probes, ValueType type, double absValFilter,
			List<Synthetic> synthCols, AsyncCallback<Integer> callback);

	public void refilterData(DataFilter filter, List<BarcodeColumn> columns,
			String[] probes, double absValFilter, List<Synthetic> synthCols,
			AsyncCallback<Integer> callback);

	public void datasetItems(int offset, int size, int sortColumn,
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);

	public void getFullData(DataFilter filter, List<String> barcodes,
			String[] probes, ValueType type, boolean sparseRead,
			boolean withSymbols,
			AsyncCallback<List<ExpressionRow>> callback);

	public void prepareCSVDownload(AsyncCallback<String> callback);

	public void getGenes(int limit, AsyncCallback<String[]> callback);
	
	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test,
			AsyncCallback<Void> callback);
	
	public void removeTwoGroupTests(AsyncCallback<Void> callback);
	
}
