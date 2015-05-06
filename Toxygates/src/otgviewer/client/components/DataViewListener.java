package otgviewer.client.components;

import java.util.List;

import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import t.common.shared.Dataset;
import t.common.shared.SampleClass;
import t.viewer.shared.ItemList;

public interface DataViewListener {
	public void datasetsChanged(Dataset[] ds);
	
	public void sampleClassChanged(SampleClass sc);
	
	public void probesChanged(String[] probes);
	
	public void compoundChanged(String compound);
	
	public void compoundsChanged(List<String> compounds);
	
	public void availableCompoundsChanged(List<String> compounds);
	
	public void columnsChanged(List<Group> columns);
	
	public void customColumnChanged(OTGColumn column);
	
	public void itemListsChanged(List<ItemList> lists);
	
}
