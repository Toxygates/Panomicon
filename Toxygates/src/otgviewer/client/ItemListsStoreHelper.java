/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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
package otgviewer.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;

import otgviewer.client.components.Screen;
import otgviewer.client.components.StorageParser;
import t.common.shared.ItemList;
import t.common.shared.StringList;

public abstract class ItemListsStoreHelper {

  protected Screen screen;
  protected String type;

  protected Collection<StringList> predefinedLists;
  protected Map<String, Map<String, ItemList>> itemLists; // { Type -> { Name -> ItemList } }

  protected DialogBox inputDialog;

  public ItemListsStoreHelper(String type, Screen screen) {
    this.screen = screen;
    this.type = type;
    this.predefinedLists = screen.manager().appInfo().predefinedProbeLists();
    this.itemLists = new HashMap<String, Map<String, ItemList>>();

    init();
  }

  /*
   * Obtain current item lists
   */
  protected void init() {
    for (ItemList il : screen.chosenItemLists) {
      if (il instanceof StringList) {
        StringList sl = (StringList) il;
        putIfAbsent(sl.type()).put(sl.name(), sl);
      }
    }
  }

  protected Map<String, ItemList> putIfAbsent(String type) {
    Map<String, ItemList> value = itemLists.get(type);
    if (value == null) {
      itemLists.put(type, new HashMap<String, ItemList>());
      value = itemLists.get(type);
    }
    return value;
  }

  /*
   * Check whether if same list type and same title is contained in the local storage.
   */
  public boolean contains(String listType, String title) {
    if (itemLists.containsKey(listType) && itemLists.get(listType).containsKey(title)) {
      return true;
    }
    return false;
  }

  protected boolean isContainedInPredefinedLists(String listType, String name) {
    for (StringList sl : predefinedLists) {
      if (sl.type().equals(listType) && sl.name().equals(name)) {
        return true;
      }
    }
    return false;
  }
  
  protected boolean validate(String name) {
    return validate(name, false);
  }

  protected boolean validate(String name, boolean overwrite) {
    if (name == null) {
      return false;
    }
    if (name.equals("")) {
      Window.alert("You must enter a non-empty name.");
      return false;
    }
    if (!StorageParser.isAcceptableString(name, "Unacceptable list name.")) {
      return false;
    }
    if (isContainedInPredefinedLists(type, name)) {
      Window.alert("This name is reserved for the system and cannot be used.");
      return false;
    }
    if (!overwrite && contains(type, name)) {
      Window.alert(
          "The title \"" + name + "\" is already taken.\n" + "Please choose a different name.");
      return false;
    }
    return true;
  }

  protected List<ItemList> buildItemLists() {
    List<ItemList> lists = new ArrayList<ItemList>();
    for (Map<String, ItemList> e : itemLists.values()) {
      lists.addAll(e.values());
    }
    return lists;
  }

}
