package otgviewer.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ListBox;

public abstract class MultiSelectionHandler<T> extends ListSelectionHandler<T> {

	protected List<T> lastMultiSelection = new ArrayList<T>();
	
	public MultiSelectionHandler(String description, ListBox list) {
		super(description, list, false);
	}
	
	List<T> lastMultiSelection() {
		return lastMultiSelection;
	}
	
	@Override
	protected void getUpdates(Object lastSelected) {
		// TODO Auto-generated method stub

	}
	
	protected void clear() {
		super.clear();
		lastMultiSelection.clear();
	}
	
	protected ChangeHandler makeChangeHandler() {
		return new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				for (ListSelectionHandler<?> handler : afterHandlers) {
					handler.clear();
				}
				lastMultiSelection.clear();
				
				for (int i = 0; i < list.getItemCount(); ++i) {
					if (list.isItemSelected(i)) {
						lastMultiSelection.add(lastResult[i]);
					}
				}
				
				int sel = list.getSelectedIndex();
				if (sel != -1) {
					lastSelected = lastResult[sel];
				} else {
					lastSelected = null;
				}
				getUpdates(lastSelected);
				getUpdates(lastMultiSelection);
			}
		};
	}
	
	public void setSelection(List<String> items) {
		
		int n = list.getItemCount();
		
		for (int i = 0; i < n; ++ i) {
			if (items.contains(list.getItemText(i))) {
				list.setItemSelected(i, true);
			} else {
				list.setItemSelected(i, false);
			}
		}
		getUpdates(items);
	}
	
	protected abstract void getUpdates(List<T> selection);

}
