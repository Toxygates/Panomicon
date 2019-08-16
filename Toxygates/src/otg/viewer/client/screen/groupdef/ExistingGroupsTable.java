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

package otg.viewer.client.screen.groupdef;

import java.util.Set;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextButtonCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;

import otg.viewer.client.screen.groupdef.GroupInspector.ButtonCellResources;
import t.common.client.components.SelectionTable;
import t.common.shared.sample.Group;
import t.model.sample.CoreParameter;

public class ExistingGroupsTable extends SelectionTable<Group> {
  private Delegate delegate;
  
  public interface Delegate {
    /**
     * Create columns that show information about groups and add those columns
     * to a table.
     * @param table the table to add columns to
     */
    void makeGroupColumns(CellTable<Group> table);
    /**
     * Prepare to edit a sample group , loading it into the UI so the user can 
     * view and modify it.
     * @param name the name of the group to be edited
     */
    void displayGroupForEditing(String name);
    /**
     * Delete a sample group
     * @param name the name of the group to be deleted
     */
    void deleteGroup(String name);
    /**
     * Notify delegate that the set of selected groups has changed.
     * @param selected the new set of selected groups
     */
    void existingGroupsTableSelectionChanged(Set<Group> selected);
  }
  
  public ExistingGroupsTable(Delegate delegate) {
    super("Active", false);
    this.delegate = delegate;

    TextColumn<Group> textColumn = new TextColumn<Group>() {
      @Override
      public String getValue(Group object) {
        return object.getName();
      }
    };
    table.addColumn(textColumn, "Group");

    textColumn = new TextColumn<Group>() {
      @Override
      public String getValue(Group object) {
        return object.getSamples()[0].get(CoreParameter.Type);
      }
    };
    table.addColumn(textColumn, "Type");

    delegate.makeGroupColumns(table);

    ButtonCellResources resources = GWT.create(ButtonCellResources.class);
    TextButtonCell.Appearance appearance = new TextButtonCell.DefaultAppearance(resources);

    // We use TextButtonCell instead of ButtonCell since it has setEnabled
    final TextButtonCell editCell = new TextButtonCell(appearance);

    Column<Group, String> editColumn = new Column<Group, String>(editCell) {
      @Override
      public String getValue(Group g) {
        return "Edit";
      }
    };
    editColumn.setFieldUpdater(new FieldUpdater<Group, String>() {
      @Override
      public void update(int index, Group object, String value) {
        delegate.displayGroupForEditing(object.getName());
      }
    });
    table.addColumn(editColumn, "");

    final TextButtonCell deleteCell = new TextButtonCell(appearance);
    Column<Group, String> deleteColumn = new Column<Group, String>(deleteCell) {
      @Override
      public String getValue(Group g) {
        return "Delete";
      }
    };
    deleteColumn.setFieldUpdater(new FieldUpdater<Group, String>() {
      @Override
      public void update(int index, Group object, String value) {
        if (Window.confirm("Are you sure you want to delete the group " + object.getName() + "?")) {
          delegate.deleteGroup(object.getName());
        }
      }

    });
    table.addColumn(deleteColumn, "");
  }

  @Override
  protected void initTable(CellTable<Group> table) {
  }

  @Override
  protected void selectionChanged(Set<Group> selected) {
    delegate.existingGroupsTableSelectionChanged(selected);
  }
}
