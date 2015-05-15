package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import otgviewer.client.Utils;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.client.dialog.InputDialog;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

/**
 * The ListChooser allows the user to select from a range of named lists,
 * as well as save and delete their own lists.
 * 
 * TODO: consider implementing SetEditor
 * @author johan
 *
 */
public class ListChooser extends DataListenerWidget {
	
	//ordered map
	private Map<String, List<String>> lists = new TreeMap<String, List<String>>();
	
	private Map<String, List<String>> predefinedLists =
			new TreeMap<String, List<String>>(); //ordered map
	private List<String> currentItems;
	private DialogBox inputDialog;
	final private ListBox listBox;
	final private String listType;
	private List<ItemList> otherTypeLists = new ArrayList<ItemList>();
	
	public ListChooser(Collection<StringList> predefinedLists, String listType) {
		HorizontalPanel hp = Utils.mkWidePanel();
		initWidget(hp);
		this.listType = listType;
		
		for (StringList sl: predefinedLists) {
			List<String> is = Arrays.asList(sl.items());
			lists.put(sl.name(), is);
			this.predefinedLists.put(sl.name(), is);
		}
		
		listBox = new ListBox();
		listBox.setVisibleItemCount(1);
		refreshSelector();
		
		listBox.setWidth("100%");
		listBox.addChangeHandler(new ChangeHandler() {				
			@Override
			public void onChange(ChangeEvent event) {
				int idx = listBox.getSelectedIndex();
				if (idx == -1) {
					return;
				}
				String sel = listBox.getItemText(idx);
				if (lists.containsKey(sel)) {
					currentItems = lists.get(sel);
					itemsChanged(currentItems);
				}
			}
		});
		hp.add(listBox);
		
		Button b = new Button("Save");
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				preSaveAction();		
			}
		});
		hp.add(b);
		
		b = new Button("Delete");
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				deleteAction();				
			}
		});
		hp.add(b);
	}
	
	protected void preSaveAction() {
		saveAction();
	}
	
	public void saveAction() {
		InputDialog entry = new InputDialog("Please enter a name for the list.") {
			@Override
			protected void onChange(String value) {
				try {				
					if (value == null) {
						return;
					}
					if (value.trim().equals("")) {
						Window.alert("You must enter a non-empty name.");
						return;
					}
					if (isPredefinedListName(value)) {
						Window.alert("This name is reserved for the system and cannot be used.");
						return;
					}
					if (!StorageParser.isAcceptableString(value,
							"Unacceptable list name.")) {
						return;
					}
					// Passed all the checks
					lists.put(value.trim(), currentItems);
					refreshSelector();
					listsChanged(getLists());
				} finally {
					inputDialog.setVisible(false);
				}
			}
		};				
		inputDialog = Utils.displayInPopup("Name entry", entry, DialogPosition.Center); 	
	}
	
	protected void deleteAction() {
		int idx = listBox.getSelectedIndex();
		if (idx == -1) {
			Window.alert("You must select a list first.");
			return;
		}
		String sel = listBox.getItemText(idx);
		if (lists.containsKey(sel)) {
			if (isPredefinedListName(sel)) {
				Window.alert("Cannot delete a predefined list.");
			} else {
				//should probably have confirmation here
				lists.remove(sel);
				refreshSelector();
				listsChanged(getLists());
			}
		}				
	}
	
	private void refreshSelector() {
		listBox.clear();
		listBox.addItem("Click to see available lists");
		for (String s: lists.keySet()) {
			listBox.addItem(s);
		}
	}
	
	protected boolean isPredefinedListName(String name) {
		return predefinedLists.containsKey(name);
	}
	
	/**
	 * To be overridden by subclasses/users.
	 * Called when the user has triggered a change.
	 */
	protected void itemsChanged(List<String> items) { }	
	
	/**
	 * To be overridden by subclasses/users.
	 * Called when the user has saved or deleted a list.
	 * @param lists
	 */
	protected void listsChanged(List<ItemList> lists) { }
	
	/**
	 * To be called by users when the current list has been edited
	 * externally.
	 * @param items
	 */
	public void setItems(List<String> items) {
		currentItems = items;
	}
	
	/**
	 * Returns all ItemLists, including the ones of a type not managed by this chooser.
	 * @return
	 */
	public List<ItemList> getLists() {
		List<ItemList> r = new ArrayList<ItemList>();
		r.addAll(otherTypeLists);
		for (String k: lists.keySet()) {
			if (!isPredefinedListName(k)) {
				List<String> v = lists.get(k);
				ItemList il = new StringList(listType, k, v.toArray(new String[0]));
				r.add(il);
			}
		}
		return r;
	}
	
	/**
	 * Set all ItemLists. This chooser will identify the ones that have the correct type
	 * and display those only.
	 * @param itemLists
	 */
	public void setLists(List<ItemList> itemLists) {
		lists.clear();
		otherTypeLists.clear();		
		for (ItemList il : itemLists) {
			if (il.type().equals(listType) && (il instanceof StringList)) {
				StringList sl = (StringList) il;
				lists.put(il.name(), Arrays.asList(sl.items()));
			} else {
				otherTypeLists.add(il);
			}			
		}
		lists.putAll(predefinedLists);
		refreshSelector();
	}

}
