package gwttest.client;

import gwttest.shared.ExpressionRow;
import gwttest.shared.ValueType;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("KC")
public interface KCService extends RemoteService {

	public List<ExpressionRow> absoluteValues(String barcode);	
	public List<ExpressionRow> foldValues(String barcode);
	
	public int loadDataset(List<String> barcodes, String[] probes, ValueType type);
	public List<ExpressionRow> datasetItems(int offset, int size);
}
