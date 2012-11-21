package otgviewer.client;

import java.util.List;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Series;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface KCServiceAsync {

	public void identifiersToProbes(DataFilter filter, String[] identifiers,
			boolean precise, AsyncCallback<String[]> callback);

	public void loadDataset(DataFilter filter, List<DataColumn> columns,
			String[] probes, ValueType type, double absValFilter,
			List<Synthetic> synthCols, AsyncCallback<Integer> callback);

	public void refilterData(DataFilter filter, List<DataColumn> columns,
			double absValFilter, List<Synthetic> synthCols,
			AsyncCallback<Integer> callback);

	public void datasetItems(int offset, int size, int sortColumn,
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);

	public void getFullData(DataFilter filter, List<String> barcodes,
			String[] probes, ValueType type, boolean sparseRead,
			AsyncCallback<List<ExpressionRow>> callback);

	public void prepareCSVDownload(AsyncCallback<String> callback);

	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test,
			AsyncCallback<Void> callback);

	public void getSingleSeries(DataFilter filter, String probe,
			String timeDose, String compound, AsyncCallback<Series> callback);

	public void getSeries(DataFilter filter, String[] probes, String timeDose,
			String[] compounds, AsyncCallback<List<Series>> callback);
	
	
}
