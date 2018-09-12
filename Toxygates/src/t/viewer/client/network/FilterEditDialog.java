package t.viewer.client.network;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.Screen;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.table.ETMatrixManager;
import t.viewer.client.table.ExpressionColumn;

public class FilterEditDialog extends InteractionDialog {
  private VerticalPanel verticalPanel;

  ListBox columnSelector;
  private Map<String, Integer> columnIndexMap;

  public FilterEditDialog(AbstractCellTable<ExpressionRow> grid, ETMatrixManager matrix, Screen parent) {
    super(parent);
    verticalPanel = new VerticalPanel();
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
    Panel editPanel = Utils.mkHorizontalPanel(true, columnSelector, editButton);
    verticalPanel.add(editPanel);

    Button closeButton = new Button("Close", new ClickHandler() {
      @Override
      public void onClick(ClickEvent e) {
        userCancel();
      }
    });

    verticalPanel.add(closeButton);
  }

  @Override
  protected Widget content() {
    return verticalPanel;
  }

}
