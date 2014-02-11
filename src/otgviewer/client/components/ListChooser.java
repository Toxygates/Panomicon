package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.Utils;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.client.dialog.InputDialog;
import otgviewer.shared.ItemList;
import otgviewer.shared.StringList;

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
	
	private HashMap<String, List<String>> lists = new HashMap<String, List<String>>();
	private Map<String, List<String>> predefinedLists;
	private List<String> currentItems;
	private DialogBox inputDialog;
	final private ListBox listBox;
	
	public ListChooser(Map<String, List<String>> predefinedLists) {
		HorizontalPanel hp = Utils.mkWidePanel();
		initWidget(hp);

		lists.putAll(predefinedLists);
		this.predefinedLists = predefinedLists;
		
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
					setItems(currentItems);
				}
			}
		});
		hp.add(listBox);
		
		Button b = new Button("Save");
		b.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				InputDialog entry = new InputDialog("Please enter a name for the list.") {
					@Override
					protected void onChange(String value) {
						if (value == null) {
							return;
						}
						if (!value.trim().equals("")) {
							if (!isPredefinedListName(value)) {
								lists.put(value, currentItems);
								refreshSelector();
								listsChanged(getLists());
							} else {
								Window.alert("This name is reserved for the system and cannot be used.");
							}
						} else {
							Window.alert("You must enter a non-empty name.");
						}

						inputDialog.setVisible(false);
					}
				};				
				inputDialog = Utils.displayInPopup("Name entry", entry, DialogPosition.Center); 				
			}
		});
		hp.add(b);
		
		b = new Button("Delete");
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
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
		});
		hp.add(b);
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
	protected void setItems(List<String> items) { }	
	
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
	public void itemsChanged(List<String> items) {
		currentItems = items;
	}
	
	public List<ItemList> getLists() {
		List<ItemList> r = new ArrayList<ItemList>();
		for (String k: lists.keySet()) {
			if (!isPredefinedListName(k)) {
				List<String> v = lists.get(k);
				ItemList il = new StringList(k, "compounds", v.toArray(new String[0]));
				r.add(il);
			}
		}
		return r;
	}
	
	public void setLists(List<ItemList> itemLists) {
		lists.clear();
		lists.putAll(predefinedLists);
		for (ItemList il: itemLists) {
			if (il instanceof StringList) {
				StringList sl = (StringList) il;
				lists.put(il.name(), Arrays.asList(sl.items()));
			}
		}
		refreshSelector();
	}

}
