package t.viewer.client.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.ui.*;

import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;

/**
 * Tools for navigating and manipulating an ExpresionTable.
 *
 */
class NavigationTools extends Composite {
  /**
   * Initial number of items to show per page at a time (but note that this number can be adjusted
   * by the user in the 0-250 range)
   */
  final static int INIT_PAGE_SIZE = 50;
  final static int MAX_PAGE_SIZE = 250;
  final static int PAGE_SIZE_INCREMENT = 50;

  protected ListBox tableList = new ListBox();
  
  private CheckBox pValueCheck;

  private HorizontalPanel tools;
  
  NavigationTools(ExpressionTable table, DataGrid<ExpressionRow> grid,
    boolean withPValueOption) {    
    tools = Utils.mkHorizontalPanel();
    initWidget(tools);
    
    HorizontalPanel horizontalPanel = Utils.mkHorizontalPanel(true);
    horizontalPanel.addStyleName("colored");
    horizontalPanel.addStyleName("slightlySpaced");
    tools.add(horizontalPanel);

    tableList.setVisibleItemCount(1);
    horizontalPanel.add(tableList);
    initTableList();
    
    tableList.addChangeHandler(e -> {      
        table.removeTests();
        table.chosenValueType = getValueType();
        table.getExpressions();
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

    PageSizePager pager = new PageSizePager(PAGE_SIZE_INCREMENT) {
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
          onPValueChange(pValueCheck.getValue()));                  
    }
    grid.setPageSize(INIT_PAGE_SIZE);

    pager.setDisplay(grid);
  }
  
  void setPValueState(boolean newState) {
    pValueCheck.setValue(newState);
  }
  
  void onPValueChange(boolean newState) {
    
  }
  
  ValueType getValueType() {
    String vt = tableList.getItemText(tableList.getSelectedIndex());
    return ValueType.unpack(vt);
  }
  
  protected void initTableList() {
    tableList.addItem(ValueType.Folds.toString());
    tableList.addItem(ValueType.Absolute.toString());
  }

  void setEnabled(boolean enabled) {
    Utils.setEnabled(tools, enabled);
  }

}
