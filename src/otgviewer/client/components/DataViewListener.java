package otgviewer.client.components;

import java.util.List;

import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.ValueType;

public interface DataViewListener {

	public void dataFilterChanged(DataFilter filter);	
	public void probesChanged(String[] probes);
	public void compoundChanged(String compound);
	public void compoundsChanged(List<String> compounds);
	public void availableCompoundsChanged(List<String> compounds);
	public void valueTypeChanged(ValueType type);
	public void columnsChanged(List<Group> columns);
	public void customColumnChanged(BarcodeColumn column);
//	public void heightChanged(int newHeight);
}
