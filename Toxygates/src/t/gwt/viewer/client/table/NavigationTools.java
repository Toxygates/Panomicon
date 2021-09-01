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

package t.gwt.viewer.client.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.ui.*;

import t.shared.common.ValueType;
import t.shared.common.sample.ExpressionRow;
import t.gwt.viewer.client.Analytics;
import t.gwt.viewer.client.Utils;

/**
 * Tools for navigating and manipulating an ExpresionTable.
 *
 */
public class NavigationTools extends Composite {
  /**
   * Initial number of items to show per page at a time (but note that this number can be adjusted
   * by the user in the 0-250 range)
   */
  public final static int INIT_PAGE_SIZE = 50;
  final static int MAX_PAGE_SIZE = 250;
  final static int PAGE_SIZE_INCREMENT = 50;

  protected ListBox valueTypeListBox = new ListBox();
  protected PageSizePager pager;
  
  public CheckBox pValueCheck;

  private HorizontalPanel tools;

  public interface Delegate {
    void setPValueDisplay(boolean newState);
    void navigationToolsValueTypeChanged();
  }
  
  NavigationTools(DataGrid<ExpressionRow> grid,
      boolean withPValueOption, Delegate delegate) {
    tools = Utils.mkHorizontalPanel();
    initWidget(tools);
    
    HorizontalPanel horizontalPanel = Utils.mkHorizontalPanel(true);
    horizontalPanel.addStyleName("colored");
    horizontalPanel.addStyleName("slightlySpaced");
    tools.add(horizontalPanel);

    valueTypeListBox.setVisibleItemCount(1);
    horizontalPanel.add(valueTypeListBox);
    initTableList();
    
    valueTypeListBox.addChangeHandler(e -> {      
        delegate.navigationToolsValueTypeChanged();
      });
    
    SimplePager.Resources res = GWT.create(SimplePager.Resources.class);

    SimplePager sp = new SimplePager(TextLocation.CENTER, res, true, 500, true) {
      @Override
      public void nextPage() {
        super.nextPage();
        Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_PAGE_CHANGE);
      }

      @Override
      public void previousPage() {
        super.previousPage();
        Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_PAGE_CHANGE);
      }

      @Override
      public void setPage(int index) {
        super.setPage(index);
        Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_PAGE_CHANGE);
      }
    };
    sp.addStyleName("slightlySpaced");
    horizontalPanel.add(sp);
    sp.setDisplay(grid);

    pager = new PageSizePager(PAGE_SIZE_INCREMENT) {
      @Override
      protected void onRangeOrRowCountChanged() {
        super.onRangeOrRowCountChanged();
        if (getPageSize() > MAX_PAGE_SIZE) {
          setPageSize(MAX_PAGE_SIZE);
        }
      }
    };

    pager.addStyleName("slightlySpaced");
    horizontalPanel.add(pager);

    if (withPValueOption) {
      pValueCheck = new CheckBox("p-value columns");
      horizontalPanel.add(pValueCheck);
      pValueCheck.setValue(false);      
      pValueCheck.addClickHandler(e ->        
      delegate.setPValueDisplay(pValueCheck.getValue()));
    }
    grid.setPageSize(INIT_PAGE_SIZE);

    pager.setDisplay(grid);
  }
  
  void setPValueState(boolean newState) {
    pValueCheck.setValue(newState);
  }
  
  ValueType getValueType() {
    String vt = valueTypeListBox.getItemText(valueTypeListBox.getSelectedIndex());
    return ValueType.unpack(vt);
  }
  
  protected void initTableList() {
    valueTypeListBox.addItem(ValueType.Folds.toString());
    valueTypeListBox.addItem(ValueType.Absolute.toString());
  }

  void setEnabled(boolean enabled) {
    Utils.setEnabled(tools, enabled);
  }
  
  int pageSize() {
    return pager.getPageSize();
  }

}
