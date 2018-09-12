package t.viewer.client.network;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.Screen;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.table.ETMatrixManager;
import t.viewer.client.table.ExpressionColumn;

public class FilterEditDialog extends InteractionDialog {
  private VerticalPanel verticalPanel;

  public FilterEditDialog(AbstractCellTable<ExpressionRow> grid, ETMatrixManager matrix, Screen parent) {
    super(parent);
    verticalPanel = new VerticalPanel();
    FlowPanel flowPanel = new FlowPanel();
    for (int i = 0; i < grid.getColumnCount(); i++) {
      Column<ExpressionRow, ?> column = grid.getColumn(i);
      if (column instanceof ExpressionColumn) {
        ExpressionColumn expressionColumn = (ExpressionColumn) column;
        int matrixIndex = expressionColumn.matrixColumn();
        Button button = new Button(matrix.info().columnName(matrixIndex));
        button.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            matrix.editColumnFilter(matrixIndex);
          }
        });
        flowPanel.add(button);
      }
    }
    verticalPanel.add(flowPanel);

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
