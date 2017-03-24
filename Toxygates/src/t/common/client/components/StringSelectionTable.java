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

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;

public class StringSelectionTable extends SelectionTable<String> {

  String _title;

  public StringSelectionTable(String selectColTitle, String title) {
    super(selectColTitle, true);
    _title = title;
  }

  protected void initTable(CellTable<String> table) {
    TextColumn<String> textColumn = new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return object;
      }
    };
    table.addColumn(textColumn, _title);
    table.setColumnWidth(textColumn, "12.5em");
  }
}
