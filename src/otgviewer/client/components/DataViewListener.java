package otgviewer.client.components;

import java.util.List;

import otgviewer.shared.OTGColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.ValueType;
import t.common.shared.SampleClass;
import t.viewer.shared.ItemList;

public interface DataViewListener {

	public void dataFilterChanged(DataFilter filter);	
	
	public void probesChanged(String[] probes);
	
	public void compoundChanged(String compound);
	
	public void compoundsChanged(List<String> compounds);
	
	public void availableCompoundsChanged(List<String> compounds);
	
	public void valueTypeChanged(ValueType type);
	
	public void columnsChanged(List<Group> columns);
	
	public void customColumnChanged(OTGColumn column);
	
	public void itemListsChanged(List<ItemList> lists);
	
	public void sampleClassChanged(SampleClass sc);
}
