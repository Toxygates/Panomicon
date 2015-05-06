package otgviewer.client.components;

import java.util.Collection;

import t.common.client.DataRecordSelector;
import t.common.shared.Dataset;

public class DatasetSelector extends DataRecordSelector<Dataset> {
	final static private String message = "Please select the datasets you want to work with.";
	
	public DatasetSelector(Collection<Dataset> items, Collection<Dataset> selectedItems) {
		super(items, message);
		setSelection(selectedItems);
	}
}
