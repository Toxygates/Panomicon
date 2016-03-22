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

package t.common.client.components;

import static t.common.client.Utils.makeScrolled;

import java.util.Arrays;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * A table to display simple string data.
 */
public class StringArrayTable extends Composite {

  CellTable<String[]> table = new CellTable<String[]>();

  /**
   * @param data row-major data for the table. The first row is the column headers.
   */
  public StringArrayTable(String[][] data) {
    initWidget(table);
    table.setSelectionModel(new NoSelectionModel<String[]>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    for (int i = 0; i < data[0].length; ++i) {
      t.common.client.Utils.makeColumn(table, i, data[0][i], "12em");
    }

    String[][] disp = Arrays.copyOfRange(data, 1, data.length);
    table.setRowData(Arrays.asList(disp));
    // table.setPageSize(100);
  }

  public static void displayDialog(String[][] data, String title, int width, int height) {
    final DialogBox db = new DialogBox(true, true);
    db.setText(title);
    Widget w = makeScrolled(new StringArrayTable(data));
    w.setWidth(width + "px");
    w.setHeight(height + "px");
    db.setWidget(w);
    db.show();
  }
}
