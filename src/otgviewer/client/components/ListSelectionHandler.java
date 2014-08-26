package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import t.common.shared.SharedUtils;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ListBox;

/**
 * TODO retire/update this class in the future. Old code, only used by ProbeSelector.
 *
 * @param <T>
 */
public abstract class ListSelectionHandler<T> {

	protected List<ListSelectionHandler<?>> afterHandlers = new ArrayList<ListSelectionHandler<?>>();
	protected String description;
	protected ListBox list;
	protected T[] lastResult;
	protected T lastSelected;
	protected boolean allOption;
	protected String[] referenceOrdering; //optional reference to assist sorting of items

	public ListSelectionHandler(String description, ListBox list, boolean allOption, String[] referenceOrdering) {
		this.description = description;
		this.list = list;
		this.allOption = allOption;
		this.referenceOrdering = referenceOrdering;
		
		list.addChangeHandler(makeChangeHandler());
	}
	
	public ListSelectionHandler(String description, ListBox list, boolean allOption) {
		this(description, list, allOption, null);
	}
	
	protected ChangeHandler makeChangeHandler() {
		return new ChangeHandler() {
			public void onChange(ChangeEvent event) {				
				int sel = list.getSelectedIndex();
				if (sel != -1) {
					for (ListSelectionHandler<?> handler : afterHandlers) {
						handler.clear();
					}
					if (list.getItemText(sel).equals("(All)") && allOption) {
						lastSelected = null;
					} else if (sel != -1) {
						lastSelected = lastResult[sel];
					} else {
						lastSelected = null;
					}
					getUpdates(lastSelected);
				}
				
			}
		};
	}
	
	
	void addAfter(ListSelectionHandler<?> after) {
		afterHandlers.add(after);
	}
	
	public void clear() {
		list.clear();
		lastSelected = null;
		for (ListSelectionHandler<?> handler: afterHandlers) {
			handler.clear();
		}
	}
	
	public void clearForLoad() {
		list.clear();
		lastSelected = null;
		list.addItem("(Loading ...)");
		list.setEnabled(false);
		for (ListSelectionHandler<?> handler: afterHandlers) {
			handler.clear();
			handler.setWaiting();
		}
	}
	
	public void setWaiting() {
		list.addItem("(Waiting for selection)");
		list.setEnabled(false);
	}
	
	public T lastSelected() {
		return lastSelected;
	}
	
	public AsyncCallback<T[]> retrieveCallback(DataListenerWidget w, final boolean warnIfNone) {
		return new PendingAsyncCallback<T[]>(w) {
			public void handleFailure(Throwable caught) {
				Window.alert("Unable to get " + description);
				list.clear();
			}

			public void handleSuccess(T[] result) {
				if (warnIfNone && result.length == 0) {
					Window.alert("No results were found.");
				} 
				setItems(result);				
			}
		};
	}
	
	public void setItems(Collection<T> items) {		
		setItems((T[]) items.toArray());
	}
	
	public void setItems(T[] result) {
		sort(result);
		lastResult = result;				
		list.clear();				
		for (T t: result) { 
			list.addItem(representation(t));
		}
		if (allOption) {
			list.addItem("(All)");
		}			
		list.setEnabled(true);
		handleRetrieval(result);
	}
	
	protected void sort(T[] items) {
		Arrays.sort(items, new Comparator<T>() {
			public int compare(T o1, T o2) {
				if (referenceOrdering != null) {					
					Integer i1 = SharedUtils.indexOf(referenceOrdering, representation(o1));
					Integer i2 = SharedUtils.indexOf(referenceOrdering, representation(o2));
					return i1.compareTo(i2); 					
				} else {
					String r1 = representation(o1);
					String r2 = representation(o2);
					return r1.compareTo(r2);
				}
			}		
		});
	}
	
	protected String representation(T value) {
		return value.toString();
	}
	
	protected void handleRetrieval(T[] result) {
		
	}
	
	protected abstract void getUpdates(T lastSelected);
		
}
