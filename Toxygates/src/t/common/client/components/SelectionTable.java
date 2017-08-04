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

package t.common.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

import t.common.shared.SharedUtils;

/**
 * A cell table that displays data and includes a column with checkboxes. By using the checkboxes,
 * the user can select some set of rows.
 * 
 */
abstract public class SelectionTable<T> extends Composite implements SetEditor<T> {
  private CellTable<T> table;
  private Column<T, Boolean> selectColumn;
  private Set<T> selected = new HashSet<T>();
  private ListDataProvider<T> provider = new ListDataProvider<T>();
  private static Logger logger = SharedUtils.getLogger("st");

  public SelectionTable(final String selectColTitle, boolean fixedLayout) {
    super();
    table = new CellTable<T>();
    initWidget(table);

    selectColumn = new Column<T, Boolean>(new CheckboxCell()) {
      @Override
      public Boolean getValue(T object) {
        return selected.contains(object);
      }
    };
    selectColumn.setFieldUpdater(new FieldUpdater<T, Boolean>() {
      @Override
      public void update(int index, T object, Boolean value) {
        if (value) {
          selected.add(object);
        } else {
          selected.remove(object);
        }
        selectionChanged(selected);
        table.redraw();
      }
    });

    if (fixedLayout) {
      // Fixed width lets us control column widths explicitly
      table.setWidth("100%", true);
    }
    table.addColumn(selectColumn, selectColTitle);
    if (fixedLayout) {
      table.setColumnWidth(selectColumn, "3.5em");
    }
    table.setSelectionModel(new NoSelectionModel<T>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    provider.addDataDisplay(table);
    initTable(table);
  }

  abstract protected void initTable(CellTable<T> table);

  protected void selectionChanged(Set<T> selected) {}

  public CellTable<T> table() {
    return this.table;
  }

  public Set<T> inverseSelection() {
    Set<T> r = new HashSet<T>(provider.getList());
    r.removeAll(selected);
    return r;
  }

  public void selectAll(Collection<T> selection) {
    selected.addAll(selection);
    setSelection(new HashSet<T>(selected));
  }

  public void unselectAll(Collection<T> selection) {
    selected.removeAll(selection);
    setSelection(new HashSet<T>(selected));
  }

  @Override
  public void setSelection(Collection<T> selection) {
    clearSelection();
    logger.info("Received selection " + selection.size());
    selected = new HashSet<T>(selection);
    table.redraw();
  }

  @Override
  public Set<T> getSelection() {
    return new HashSet<T>(selected);
  }

  public void select(T t) {
    logger.info("Select " + t);
    selected.add(t);
    table.redraw();
  }

  public void unselect(T t) {
    selected.remove(t);
    table.redraw();
  }

  public void addItem(T t) {
    provider.getList().add(t);
  }

  public void removeItem(T t) {
    provider.getList().remove(t);
    selected.remove(t);
  }

  @Override
  public List<T> availableItems() {
    return provider.getList();
  }

  @Override
  public Set<T> validateItems(List<T> items) {
    return new HashSet<T>(items);
  }

  @Override
  public List<Suggestion> getSuggestions(String request) {
    return new ArrayList<Suggestion>();
  }

  @Override
  public void setSelection(Collection<T> items, @Nullable SetEditor<T> fromSelector) {
    setSelection(items);
  }


  /**
   * Get an item that was selected by highlighting a row (not by ticking a check box)
   * 
   * @return
   */
  public T highlightedRow() {
    for (T t : provider.getList()) {
      if (table.getSelectionModel().isSelected(t)) {
        return t;
      }
    }
    return null;
  }

  public void clearSelection() {
    selected = new HashSet<T>();
    // reset any edits the user might have done
    for (T item : provider.getList()) {
      ((CheckboxCell) selectColumn.getCell()).clearViewData(provider.getKey(item));
    }
    table.redraw();
  }

  public void setItems(List<T> data) {
    setItems(data, true);
  }

  /**
   * TODO: retire this method
   * 
   * @param data
   * @param clearSelection
   */
  @Override
  public void setItems(List<T> data, boolean clearSelection) {
    logger.info("Set items " + data.size() + " clear: " + clearSelection);
    provider.setList(new ArrayList<T>(data));
    table.setVisibleRange(0, data.size());
    if (clearSelection) {
      clearSelection();
    } else {
      Set<T> toRemove = new HashSet<T>();
      for (T t : selected) {
        if (!data.contains(t)) {
          toRemove.add(t);
        }
      }
      selected.removeAll(toRemove);
    }
  }

  public T get(int index) {
    return provider.getList().get(index);
  }

  public List<T> getItems() {
    return provider.getList();
  }
}
