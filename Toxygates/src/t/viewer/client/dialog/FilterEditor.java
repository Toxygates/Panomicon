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

package t.viewer.client.dialog;

import javax.annotation.Nullable;

import t.common.client.components.ItemSelector;
import t.viewer.client.Utils;
import t.viewer.shared.ColumnFilter;
import t.viewer.shared.FilterType;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A dialog for displaying and modifying a column filter.
 * 
 * @author johan
 */
public class FilterEditor extends Composite {

  private TextBox input = new TextBox();
  protected int editColumn;
  private ItemSelector<FilterType> filterType;

  public FilterEditor(String columnTitle, int column,
      final ColumnFilter initValue) {
    this.editColumn = column;
    VerticalPanel vp = Utils.mkVerticalPanel(true);
    initWidget(vp);
    vp.setWidth("300px");

    Label l =
        new Label("Please choose a bound for '" + columnTitle + "'. Examples: " + formatNumber(2.1)
            + ", " + formatNumber(1.2e-3));
    l.setWordWrap(true);
    vp.add(l);

    if (initValue.threshold != null) {
      input.setValue(formatNumber(initValue.threshold));
    }
    
    filterType = new ItemSelector<FilterType>() {
      @Override
      public FilterType[] values() { return FilterType.values(); }
      
      @Override
      protected FilterType valueForTitle(String s) {
        return FilterType.parse(s);
      }
    };
    
    filterType.setSelected(initValue.filterType);

    HorizontalPanel hp = Utils.mkHorizontalPanel(true, filterType, input);
    vp.add(hp);

    final Button setButton = new Button("OK");
    setButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        try {
          Double newVal = parseNumber(input.getText());
          ColumnFilter newFilt = new ColumnFilter(newVal, filterType.value());
          onChange(newFilt);
        } catch (NumberFormatException e) {
          Window.alert("Invalid number format.");
        }
      }
    });

    input.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        setButton.click();
      }
    });

    Button clearButton = new Button("Clear filter");
    clearButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onChange(new ColumnFilter(null, filterType.value()));
      }

    });
    hp = Utils.mkHorizontalPanel(true, setButton, clearButton);
    vp.add(hp);
  }

  private NumberFormat dfmt = NumberFormat.getDecimalFormat();
  private NumberFormat sfmt = NumberFormat.getScientificFormat();

  String formatNumber(Double val) {
    if (val < 0.01 || val > 100000) {
      return sfmt.format(val);
    } else {
      return dfmt.format(val);
    }
  }

  Double parseNumber(String val) throws NumberFormatException {
    try {
      return dfmt.parse(val);
    } catch (NumberFormatException e) {
      return sfmt.parse(val);
    }
  }

  /**
   * Called when the filter is changed. To be overridden by subclasses.
   */

  protected void onChange(@Nullable ColumnFilter newFilter) {}
}
