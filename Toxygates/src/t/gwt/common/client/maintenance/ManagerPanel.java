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

package t.gwt.common.client.maintenance;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.*;

import t.gwt.common.client.Resources;
import t.gwt.common.client.RunCommand;
import t.gwt.common.client.Utils;
import t.shared.common.ManagedItem;

abstract public class ManagerPanel<T extends ManagedItem> {

  // Task: 100 is a totally arbitrary number - we should eventually have paging controls here.
  private CellTable<T> table = Utils.makeTable(100);
  List<RunCommand> cmds = new ArrayList<RunCommand>();
  private DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
  private final String type;
  protected final Resources resources;
  
  public ManagerPanel(String type, 
      Resources resources,
      boolean scrolled,
      boolean buttonsNorth) {
    this.type = type;
    this.resources = resources;
    cmds.add(new RunCommand("Add new...", () -> {
      if (confirmAddNew()) {
        showEditor(null, true);
      }
    }));

    StandardColumns<T> sc = new StandardColumns<T>(table) {
      @Override
      void onDelete(T object) {
        ManagerPanel.this.onDelete(object);
      }

      @Override
      void onEdit(T object) {
        showEditor(object, false);
      }
    };

    sc.addStartColumns();
    addMidColumns(table);
    sc.addEndColumns();

    if (!buttonsNorth) {
      dlp.addSouth(Utils.makeButtons(cmds), 35);
    }
    dlp.add(scrolled ? Utils.makeScrolled(table) : table);
  }

  protected boolean confirmAddNew() { return true; }
  
  public CellTable<T> table() { return table; }
  
  protected void showEditor(@Nullable T obj, boolean addNew) {
    final DialogBox db = new DialogBox(false, true);
    String addOrEdit = addNew ? "Add " : "Edit ";
    db.setText(addOrEdit + type);
    db.setWidget(makeEditor(obj, db, addNew));    
    db.show();
  }

  abstract protected Widget makeEditor(@Nullable T obj, DialogBox db, boolean addNew);

  abstract protected void onDelete(T object);

  protected void addMidColumns(CellTable<T> table) {}

  public List<RunCommand> commands() {
    return cmds;
  }

  public Widget panel() {
    return dlp;
  }

}
