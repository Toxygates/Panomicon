/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client.maintenance;

import static t.common.client.Utils.makeButtons;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import t.common.client.Command;
import t.common.client.Resources;
import t.common.shared.ManagedItem;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.*;

abstract public class ManagerPanel<T extends ManagedItem> {

  //TODO 100 is an arbitrary number - we should eventually have paging controls in the GUI.
  //For example, SimplePager.
  //A page size of <= 20 is best to reduce the number of queries made to the server.
  private CellTable<T> table = t.common.client.Utils.makeTable(100);
  List<Command> cmds = new ArrayList<Command>();
  private DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
  private final String editCaption;
  protected final Resources resources;
  
  public ManagerPanel(String editCaption, 
      Resources resources,
      boolean scrolled,
      boolean buttonsNorth) {
    this.editCaption = editCaption;
    this.resources = resources;
    cmds.add(new Command("Add new...") {
      public void run() {
        if (confirmAddNew()) {
          showEditor(null, true);
        }
      }
    });

    StandardColumns<T> sc = new StandardColumns<T>(table) {
      void onDelete(T object) {
        ManagerPanel.this.onDelete(object);
      }

      void onEdit(T object) {
        showEditor(object, false);
      }
    };

    sc.addStartColumns();
    addMidColumns(table);
    sc.addEndColumns();

    if (!buttonsNorth) {
      dlp.addSouth(makeButtons(cmds), 35);
    }
    dlp.add(scrolled ? t.common.client.Utils.makeScrolled(table) : table);
  }

  protected boolean confirmAddNew() { return true; }
  
  public CellTable<T> table() { return table; }
  
  protected void showEditor(@Nullable T obj, boolean addNew) {
    final DialogBox db = new DialogBox(false, true);
    db.setText(editCaption);
    db.setWidget(makeEditor(obj, db, addNew));    
    db.show();
  }

  abstract protected Widget makeEditor(@Nullable T obj, DialogBox db, boolean addNew);

  abstract protected void onDelete(T object);

  protected void addMidColumns(CellTable<T> table) {}

  public List<Command> commands() {
    return cmds;
  }

  public Widget panel() {
    return dlp;
  }

}
