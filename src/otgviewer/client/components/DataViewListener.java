package otgviewer.client.components;

import java.util.List;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ValueType;

public interface DataViewListener {

	public void dataFilterChanged(DataFilter filter);
	public void probeChanged(String probe);
	public void probesChanged(String[] probes);
	public void compoundsChanged(List<String> compounds);
	public void compoundChanged(String compound);
	public void valueTypeChanged(ValueType type);
	public void columnsChanged(List<DataColumn> columns);
	
	public void heightChanged(int newHeight);
}
