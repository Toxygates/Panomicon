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

package t.common.client;

import java.util.*;

import t.common.client.components.SelectionTable;
import t.shared.common.DataRecord;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Dialog for selecting from a list of DataRecords.
 * 
 * @param <T> the type of DataRecord being selected.
 */
public class DataRecordSelector<T extends DataRecord> extends Composite {

  protected SelectionTable<T> st;
  protected VerticalPanel vp;

  public DataRecordSelector(Collection<T> items) {
    vp = new VerticalPanel();
    initWidget(vp);

    st = new SelectionTable<T>("", true) {
      @Override
      protected void initTable(CellTable<T> table) {
        TextColumn<T> tc = new TextColumn<T>() {
          @Override
          public String getValue(T object) {
            return object.getUserTitle();
          }
        };
        table.addColumn(tc, "Title");
      }
    };
    setItems(items);
    vp.add(st);
  }

  public Set<T> getSelection() {
    return st.getSelection();
  }

  public void setItems(Collection<T> data) {
    st.setItems(new ArrayList<T>(data));
  }

  public void setSelection(Collection<T> items) {
    st.setSelection(items);
  }

  public void selectAll() {
    st.selectAll(st.getItems());
  }

}
