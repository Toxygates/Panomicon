/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import otgviewer.client.dialog.InputDialog;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * The ListChooser allows the user to select from a range of named lists, as well as save and delete
 * their own lists.
 * 
 * TODO: consider implementing SetEditor
 * 
 * @author johan
 *
 */
public class ListChooser extends DataListenerWidget {

  public static final int SAVE_SUCCESS = 0;
  public static final int SAVE_FAILURE = 1;

  private final String DEFAULT_ITEM;

  // ordered map
  protected Map<String, List<String>> lists = new TreeMap<String, List<String>>();

  private Map<String, List<String>> predefinedLists = new TreeMap<String, List<String>>(); // ordered
                                                                                           // map
  protected List<String> currentItems;
  private DialogBox inputDialog;
  final private ListBox listBox;
  final private String listType;
  private List<ItemList> otherTypeLists = new ArrayList<ItemList>();

  /*
   * Create empty list box
   */
  public ListChooser(String listType) {
    this(listType, true);
  }

  public ListChooser(String listType, String defaultItem) {
    this(listType, true, defaultItem);
  }

  public ListChooser(String listType, boolean hasButtons) {
    this(listType, true, "Click to see available lists");
  }

  public ListChooser(String listType, boolean hasButtons, String defaultItem) {
    this(new ArrayList<StringList>(), listType, hasButtons, defaultItem);
  }

  /*
   * Create list box with predefined lists
   */
  public ListChooser(Collection<StringList> predefinedLists, String listType) {
    this(predefinedLists, listType, true);
  }

  public ListChooser(Collection<StringList> predefinedLists, String listType, String defaultItem) {
    this(predefinedLists, listType, true, defaultItem);
  }

  public ListChooser(Collection<StringList> predefinedLists, String listType, boolean hasButtons) {
    this(predefinedLists, listType, hasButtons, "Click to see available lists");
  }

  public ListChooser(Collection<StringList> predefinedLists, String listType, boolean hasButtons,
      String defaultItem) {
    this.listType = listType;
    this.DEFAULT_ITEM = defaultItem;

    for (StringList sl : predefinedLists) {
      List<String> is = Arrays.asList(sl.items());
      lists.put(sl.name(), is);
      this.predefinedLists.put(sl.name(), is);
    }
    listBox = new ListBox();

    initWidget(createPanel(hasButtons));
  }


  private Widget createPanel(boolean hasButtons) {
    HorizontalPanel hp = Utils.mkWidePanel();

    listBox.setVisibleItemCount(1);
    refreshSelector();

    listBox.setWidth("100%");
    listBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        String sel = getSelectedText();
        if (sel == null) {
          return;
        }
        currentItems = lists.containsKey(sel) ? lists.get(sel) : new ArrayList<String>();
        itemsChanged(currentItems);
      }
    });
    hp.add(listBox);

    if (!hasButtons) {
      return hp;
    }

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

    return hp;
  }

  public String getSelectedText() {
    int idx = listBox.getSelectedIndex();
    if (idx == -1) {
      return null;
    }
    return listBox.getItemText(idx);
  }

  public int getSelectedIndex() {
    return listBox.getSelectedIndex();
  }

  protected void preSaveAction() {
    saveAction();
  }

  protected boolean checkName(String name) {
    if (name == null) {
      return false;
    }
    if (name.equals("")) {
      Window.alert("You must enter a non-empty name.");
      return false;
    }
    if (isPredefinedListName(name)) {
      Window.alert("This name is reserved for the system and cannot be used.");
      return false;
    }
    if (!StorageParser.isAcceptableString(name, "Unacceptable list name.")) {
      return false;
    }
    return true;
  }

  public int saveAs(String entryName, List<String> items) {
    if (entryName != null) {
      entryName = entryName.trim();
    }
    if (!checkName(entryName)) {
      return SAVE_FAILURE;
    }

    lists.put(entryName, items);
    refreshSelector();
    listsChanged(getLists());

    return SAVE_SUCCESS;
  }

  public void saveAction() {
    InputDialog entry = new InputDialog("Please enter a name for the list.") {
      @Override
      protected void onChange(String value) {
        saveAs(value, currentItems);
        inputDialog.setVisible(false);
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
        // should probably have confirmation here
        lists.remove(sel);
        refreshSelector();
        listsChanged(getLists());
      }
    }
  }

  protected void refreshSelector() {
    String selected = getSelectedText();

    listBox.clear();
    listBox.addItem(DEFAULT_ITEM);

    List<String> sorted = new ArrayList<String>(lists.keySet());
    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        if (o1.length() == o2.length()) {
          return o1.compareTo(o2);
        }
        return (o1.length() < o2.length() ? -1 : 1);
      }
    });

    int idx = 0, i = 1;
    for (String s : sorted) {
      listBox.addItem(s);
      if (s.equals(selected)) {
        idx = i;
      }
      ++i;
    }

    listBox.setSelectedIndex(idx);
  }

  protected boolean isPredefinedListName(String name) {
    return predefinedLists.containsKey(name);
  }

  public boolean containsEntry(String listType, String name) {
    return (this.listType.equals(listType) && lists.containsKey(name));
  }

  /**
   * To be overridden by subclasses/users. Called when the user has triggered a change.
   */
  protected void itemsChanged(List<String> items) {}

  /**
   * To be overridden by subclasses/users. Called when the user has saved or deleted a list.
   * 
   * @param lists
   */
  protected void listsChanged(List<ItemList> lists) {}

  /**
   * To be called by users when the current list has been edited externally.
   * 
   * @param items
   */
  public void setItems(List<String> items) {
    currentItems = items;
  }

  /**
   * Returns all ItemLists, including the ones of a type not managed by this chooser.
   * 
   * @return
   */
  public List<ItemList> getLists() {
    List<ItemList> r = new ArrayList<ItemList>();
    r.addAll(otherTypeLists);
    for (String k : lists.keySet()) {
      if (!isPredefinedListName(k)) {
        List<String> v = lists.get(k);
        ItemList il = new StringList(listType, k, v.toArray(new String[0]));
        r.add(il);
      }
    }
    return r;
  }

  /**
   * Set all ItemLists. This chooser will identify the ones that have the correct type and display
   * those only.
   * 
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

  /**
   * Try to select an item that matches given title in list box.
   * 
   * @param title
   * @return index of the item, or <tt>-1</tt> if there was no item matching given title.
   */
  public int trySelect(String title) {
    int ret = -1;

    for (int i = 0; i < listBox.getItemCount(); ++i) {
      if (listBox.getItemText(i).equals(title)) {
        listBox.setSelectedIndex(i);
        ret = i;
        break;
      }
    }

    if (ret == -1) {
      listBox.setSelectedIndex(0);
    }

    return ret;
  }

}
