package otgviewer.client;

import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.ValueType;

public interface DataViewListener {

	public void dataFilterChanged(DataFilter filter);
	public void probeChanged(String probe);
	public void compoundsChanged(List<String> compounds);
	public void compoundChanged(String compound);
	public void valueTypeChanged(ValueType type);
}
