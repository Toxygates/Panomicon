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
package otgviewer.client;

import java.util.Collection;

import otgviewer.client.components.Screen;
import otgviewer.client.dialog.InputDialog;
import t.common.shared.StringList;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;

public class StringListsStoreHelper extends ItemListsStoreHelper {

  // private final Logger logger = SharedUtils.getLogger("ItemListsStoreHelper");

  public StringListsStoreHelper(String type, Screen screen) {
    super(type, screen);
  }

  /*
   * Override this function to handle what genes were saved
   */
  protected void onSaveSuccess(String name, StringList items) {}

  /**
   * Save simple string list to local storage. An input box which asks the list's title will be
   * shown.
   * 
   * @param list
   */
  public void save(Collection<String> list) {
    saveAction(list, "Name entry", "Please enter a name for the list.");
  }

  /*
   * Save gene set with specified title. No input box will be shown.
   */
  public void saveAs(Collection<String> list, String name) {
    saveAction(list, name, false);
  }

  public void saveAs(Collection<String> list, String name, boolean overwrite) {
    saveAction(list, name, overwrite);
  }

  private void saveAction(final Collection<String> list, String caption, String message) {
    InputDialog entry = new InputDialog(message) {
      @Override
      protected void onChange(String value) {
        if (value == null) { // on Cancel clicked
          inputDialog.setVisible(false);
          return;
        }

        saveAction(list, value, false);
        inputDialog.setVisible(false);
      }
    };
    inputDialog = Utils.displayInPopup(caption, entry, DialogPosition.Center);
  }

  private void saveAction(Collection<String> list, String name, boolean overwrite) {
    if (!validate(name, overwrite)) {
      return;
    }

    StringList sl = new StringList(type, name, list.toArray(new String[0]));
    putIfAbsent(type).put(name, sl);

    storeItemLists();

    onSaveSuccess(name, sl);
  }

  private void storeItemLists() {
    screen.itemListsChanged(buildItemLists());
    screen.storeItemLists(screen.getParser());
  }

}
