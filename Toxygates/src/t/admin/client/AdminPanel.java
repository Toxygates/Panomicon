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

package t.admin.client;

import static t.common.client.Utils.makeButtons;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import t.common.client.Command;
import t.common.shared.ManagedItem;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

abstract class AdminPanel<T extends ManagedItem> {

  CellTable<T> table = AdminConsole.makeTable();
  List<Command> cmds = new ArrayList<Command>();
  private DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
  private final String editCaption;
  private final @Nullable String dialogWidth;

  AdminPanel(String editCaption, @Nullable String dialogWidth) {
    this.editCaption = editCaption;
    this.dialogWidth = dialogWidth;
    cmds.add(new Command("Add new...") {
      public void run() {
        showEditor(null, true);
      }
    });

    StandardColumns<T> sc = new StandardColumns<T>(table) {
      void onDelete(T object) {
        AdminPanel.this.onDelete(object);
      }

      void onEdit(T object) {
        showEditor(object, false);
      }
    };

    sc.addStartColumns();
    addMidColumns(table);
    sc.addEndColumns();

    dlp.addSouth(makeButtons(cmds), 35);
    dlp.add(table);
  }

  void showEditor(@Nullable T obj, boolean addNew) {
    final DialogBox db = new DialogBox(false, true);
    db.setText(editCaption);
    db.setWidget(makeEditor(obj, db, addNew));
    if (dialogWidth != null) {
      db.setWidth(dialogWidth);
    }
    db.show();
  }

  abstract Widget makeEditor(@Nullable T obj, DialogBox db, boolean addNew);

  abstract void onDelete(T object);

  void addMidColumns(CellTable<T> table) {}

  List<Command> commands() {
    return cmds;
  }

  Widget panel() {
    return dlp;
  }

}
