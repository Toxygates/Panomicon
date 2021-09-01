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

package t.gwt.common.client;

import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.*;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.NoSelectionModel;

import t.shared.common.ManagedItem;

public class Utils {

  public static Widget makeButtons(List<RunCommand> commands) {
    HorizontalPanel buttons = new HorizontalPanel();
    buttons.setSpacing(4);
    for (final RunCommand c : commands) {
      Button b = makeButton(c);
      buttons.add(b);
    }
    return buttons;
  }

  public static Button makeButton(final RunCommand c) {
    Button b = new Button(c.getTitle());
    b.addClickHandler(e -> c.run());      
    return b;
  }
  
  public static Button makeButton(String label, Runnable r) {
    Button b = new Button(label);
    b.addClickHandler(e -> r.run());      
    return b;
  }

  public static TextColumn<String[]> makeColumn(CellTable<String[]> table, final int idx,
      String title, String width) {
    TextColumn<String[]> col = new TextColumn<String[]>() {
      @Override
      public String getValue(String[] x) {
        if (x.length > idx) {
          return x[idx];
        } else {
          return "";
        }
      }
    };

    SafeHtml hhtml =
        SafeHtmlUtils.fromSafeConstant("<span title=\"" + title + "\">" + title + "</span>");
    SafeHtmlHeader header = new SafeHtmlHeader(hhtml);
    table.addColumn(col, header);
    table.setColumnWidth(col, width);
    return col;
  }

  public static ScrollPanel makeScrolled(Widget w) {
    ScrollPanel sp = new ScrollPanel(w);
    sp.setWidth("auto");
    return sp;
  }
  
  public static <T extends ManagedItem> CellTable<T> makeTable(int pageSize) {
    CellTable<T> table = new CellTable<T>(pageSize);
    table.setSelectionModel(new NoSelectionModel<T>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    return table;
  }  

  private static NumberFormat decimalFormat = NumberFormat.getDecimalFormat();
  private static NumberFormat scientificFormat = NumberFormat.getScientificFormat();

  public static String formatNumber(double v) {
    if (v == 0.0) {
      return "0";
    }
    if (Math.abs(v) > 0.001) {
      return decimalFormat.format(v);
    } else {
      return scientificFormat.format(v);
    }
  }

  public static boolean shouldHandleClickEvent(NativeEvent ev, String expectedParentId) {
    String id = clickParentId(ev);
    return (id != null && id.equals(expectedParentId));
  }
  
  public static @Nullable String clickParentId(NativeEvent ev) {
    if ("click".equals(ev.getType())) {
      EventTarget et = ev.getEventTarget();          
      if (Element.is(et)) {
        Element e = et.cast();            
        return e.getParentElement().getId();
      }
    }
    return null;
  }
}
