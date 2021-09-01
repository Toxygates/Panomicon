/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.viewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.gwt.viewer.client.Utils;
import t.gwt.viewer.client.dialog.DialogPosition;
import t.gwt.viewer.client.dialog.InputDialog;
import t.gwt.viewer.client.storage.NamedObjectStorage;
import t.shared.viewer.ItemList;
import t.shared.viewer.StringList;

/**
 * The ListChooser allows the user to select from a range of named lists, as well as save and delete
 * their own lists.
 * 
 * In the future, this class might implement SetEditor.
 */
public class ListChooser extends Composite {

  public static final int SAVE_SUCCESS = 0;
  public static final int SAVE_FAILURE = 1;

  private final String DEFAULT_ITEM;

  // ordered map
  protected Map<String, List<String>> lists = new TreeMap<String, List<String>>();
  protected Map<String, List<String>> extraLists = new TreeMap<String, List<String>>();

  private Map<String, List<String>> predefinedLists = new TreeMap<String, List<String>>(); // ordered
                                                                                           // map
  protected List<String> currentItems;
  private DialogBox inputDialog;
  final private ListBox listBox;
  final private String listType;
  
  public NamedObjectStorage<StringList> storage;

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
    HorizontalPanel hp = Utils.mkHorizontalPanel(true);
    hp.setWidth("100%");

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
        if (lists.containsKey(sel)) {
          currentItems = lists.get(sel);
        } else if (extraLists.containsKey(sel)) {
          currentItems = extraLists.get(sel);
        } else if (predefinedLists.containsKey(sel)) {
          currentItems = predefinedLists.get(sel);
        } else {
          currentItems = new ArrayList<String>();
        }
        itemsChanged(currentItems);
      }
    });
    hp.add(listBox);

    if (!hasButtons) {
      return hp;
    }

    Button b = new Button("Save");
    b.addStyleName("lightButton");
    b.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        preSaveAction();
      }
    });
    hp.add(b);

    b = new Button("Delete");
    b.addStyleName("lightButton");
    b.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        deleteAction();
      }
    });
    hp.add(b);

    return hp;
  }
  
  public void resetSelection() {
    listBox.setSelectedIndex(0);
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
    int newItemIndex = Utils.findListBoxItemIndex(listBox, entryName);
    listBox.setSelectedIndex(newItemIndex);

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
    Logger.getLogger("aou").info("idx =  " + idx + "; lists.size = " + lists.size());
    if (isPredefinedListName(sel)) {
      Window.alert("Cannot delete a predefined list.");
    } else if (idx > lists.size()) {
      Window.alert("Cannot delete extra lists.");
    } else if (lists.containsKey(sel)) {
      // should probably have confirmation here
      lists.remove(sel);
      refreshSelector();
      listsChanged(getLists());
    } else {
      throw new RuntimeException("Error finding list box item text in list of items.");
    }
  }

  protected void refreshSelector() {
    listBox.clear();
    listBox.addItem(DEFAULT_ITEM);

    addItems(lists);
    addItems(extraLists);
    addItems(predefinedLists);
  }
  
  protected void addItems(Map<String, List<String>> listsToAdd) {
    String selected = getSelectedText();
    
    List<String> sorted = new ArrayList<String>(listsToAdd.keySet());
    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        if (o1.length() == o2.length()) {
          return o1.compareTo(o2);
        }
        return (o1.length() < o2.length() ? -1 : 1);
      }
    });

    int idx = -1, i = 1;
    for (String s : sorted) {
      listBox.addItem(s);
      if (s.equals(selected)) {
        idx = i;
      }
      ++i;
    }

    if (idx >= 0) {
      listBox.setSelectedIndex(idx);
    }
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

  protected void listsChanged(List<StringList> lists) {
    if (storage != null) {
      storage.clear();
      storage.insertAll(lists, true);
      storage.saveToStorage();
    }
  }
  
  /**
   * Checks the validity of the name for a new list. Should be overridden
   * if hasButtons = true.
   * @param name
   * @return
   */
  protected boolean checkName(String name) {
    if (storage != null) {
      return storage.validateNewObjectName(name, false);
    } else {
      return false;
    }
  }

  /**
   * To be called by users when the current list has been edited externally.
   */
  public void setItems(List<String> items) {
    currentItems = items;
  }

  /**
   * Returns the lists managed by this chooser.
   */
  public List<StringList> getLists() {
    List<StringList> r = new ArrayList<StringList>();
    for (String k : lists.keySet()) {
      if (!isPredefinedListName(k)) {
        List<String> v = lists.get(k);
        StringList il = new StringList(listType, k, v.toArray(new String[0]));
        r.add(il);
      }
    }
    return r;
  }

  /**
   * Set all ItemLists. This chooser will identify the ones that have the correct type and display
   * those only.
   */
  public <T extends ItemList> void setLists(List<T> itemLists) {
    lists.clear();

    for (T il : itemLists) {
      if (il.type().equals(listType) && (il instanceof StringList)) {
        StringList sl = (StringList) il;
        lists.put(il.name(), Arrays.asList(sl.items()));
      }
    }
    
    refreshSelector();
  }

  public void setExtraLists(List<? extends ItemList> newExtraLists, String namePrefix) {
    extraLists.clear();
    
    for (ItemList il : newExtraLists) {
      if ((il instanceof StringList)) {
        StringList sl = (StringList) il;
        extraLists.put(namePrefix + il.name(), Arrays.asList(sl.items()));
      }
    }
    refreshSelector();
  }
  
  /**
   * Try to select an item that matches given title in list box.
   * 
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
