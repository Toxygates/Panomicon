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

package t.common.client.maintenance;

import java.util.Date;

import t.common.shared.DataRecord;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;

abstract public class StandardColumns<T extends DataRecord> {

  final protected CellTable<T> table;

  StandardColumns(CellTable<T> table) {
    this.table = table;
  }

  abstract class SimpleButtonColumn {
    ButtonCell cell;
    Column<T, String> column;

    SimpleButtonColumn(final String text) {
      cell = new ButtonCell();
      column = new Column<T, String>(cell) {
        public String getValue(T b) {
          return text;
        }
      };

      column.setFieldUpdater(new FieldUpdater<T, String>() {
        @Override
        public void update(int index, T object, String value) {
          onClick(object);
        }
      });
    }

    abstract void onClick(T object);
  }

  void addStartColumns() {
    TextColumn<T> textColumn = new TextColumn<T>() {
      @Override
      public String getValue(T object) {
        return object.getTitle();
      }
    };
    table.addColumn(textColumn, "ID");
    table.setColumnWidth(textColumn, "12.5em");

    TextColumn<T> commentColumn = new TextColumn<T>() {
      @Override
      public String getValue(T object) {
        return object.getComment();
      }
    };
    table.addColumn(commentColumn, "Comment");

    TextColumn<T> dateColumn = new TextColumn<T>() {
      @Override
      public String getValue(T object) {
        Date d = object.getDate();
        if (d != null) {
          String time = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_SHORT).format(d);
          return time;
        } else {
          return "N/A";
        }
      }
    };
    table.addColumn(dateColumn, "Added");
  }

  void addEndColumns() {
    SimpleButtonColumn edit = new SimpleButtonColumn("Edit...") {
      void onClick(T obj) {
        onEdit(obj);
      }
    };
    SimpleButtonColumn delete = new SimpleButtonColumn("Delete") {
      void onClick(T obj) {
        onDelete(obj);
      }
    };
    table.addColumn(edit.column);
    table.addColumn(delete.column);
  }

  abstract void onEdit(T obj);

  abstract void onDelete(T obj);
}
