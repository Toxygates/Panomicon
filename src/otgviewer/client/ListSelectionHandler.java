package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ListBox;

public abstract class ListSelectionHandler<T> {

	protected List<ListSelectionHandler<?>> afterHandlers = new ArrayList<ListSelectionHandler<?>>();
	protected String description;
	protected ListBox list;
	protected T[] lastResult;
	protected T lastSelected;
	protected boolean allOption;

	ListSelectionHandler(String description, ListBox list, boolean allOption) {
		this.description = description;
		this.list = list;
		this.allOption = allOption;
		
		list.addChangeHandler(makeChangeHandler());
	}
	
	protected ChangeHandler makeChangeHandler() {
		return new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				for (ListSelectionHandler<?> handler : afterHandlers) {
					handler.clear();
				}
				int sel = list.getSelectedIndex();
				
				if (list.getItemText(sel).equals("(All)") && allOption) {
					lastSelected = null;
				} else if (sel != -1) {
					lastSelected = lastResult[sel];
				} else {
					lastSelected = null;
				}
				
				getUpdates(lastSelected);
			}
		};
	}
	
	
	void addAfter(ListSelectionHandler after) {
		afterHandlers.add(after);
	}
	
	void clear() {
		list.clear();
		lastSelected = null;
		for (ListSelectionHandler<?> handler: afterHandlers) {
			handler.clear();
		}
	}
	
	public T lastSelected() {
		return lastSelected;
	}
	
	AsyncCallback<T[]> retrieveCallback() {
		return new AsyncCallback<T[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get " + description);
			}

			public void onSuccess(T[] result) {
				lastResult = result;
				list.clear();
				for (T t: result) {
					list.addItem(t.toString());
				}
				if (allOption) {
					list.addItem("(All)");
				}
				handleRetreival(result);				
			}
		};
	}
	
	protected void handleRetreival(T[] result) {
		
	}
	
	protected abstract void getUpdates(T lastSelected);
		
}
