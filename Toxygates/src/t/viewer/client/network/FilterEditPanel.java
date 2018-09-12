package t.viewer.client.network;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.*;

import t.common.shared.sample.ExpressionRow;
import t.viewer.client.Utils;
import t.viewer.client.table.ETMatrixManager;
import t.viewer.client.table.ExpressionColumn;

public class FilterEditPanel {
  private HorizontalPanel horizontalPanel;
  private ListBox columnSelector;
  private Map<String, Integer> columnIndexMap;

  public FilterEditPanel(AbstractCellTable<ExpressionRow> grid, ETMatrixManager matrix) {
    columnIndexMap = new HashMap<String, Integer>();
    columnSelector = new ListBox();
    for (int i = 0; i < grid.getColumnCount(); i++) {
      Column<ExpressionRow, ?> column = grid.getColumn(i);
      if (column instanceof ExpressionColumn) {
        ExpressionColumn expressionColumn = (ExpressionColumn) column;
        int matrixIndex = expressionColumn.matrixColumn();
        String columnTitle = matrix.info().columnName(matrixIndex);
        columnSelector.addItem(columnTitle);
        columnIndexMap.put(columnTitle, matrixIndex);
      }
    }
    Button editButton = new Button("Edit");
    editButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        matrix.editColumnFilter(columnIndexMap.get(columnSelector.getSelectedItemText()));
      }
    });
    horizontalPanel = Utils.mkHorizontalPanel(true, new Label("Column filters:"), columnSelector, editButton);
  }

  protected Widget content() {
    return horizontalPanel;
  }
}
