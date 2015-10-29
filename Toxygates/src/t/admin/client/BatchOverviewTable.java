package t.admin.client;

import java.util.Arrays;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * A table to display summary information about the samples in a batch.
 */
class BatchOverviewTable extends Composite {
  
  CellTable<String[]> table = new CellTable<String[]>();
  
  /**
   * @param data row-major data for the table. The first row is the column headers.
   */
  BatchOverviewTable(String[][] data) {
    initWidget(table);
    table.setSelectionModel(new NoSelectionModel<String[]>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    
    for (int i = 0; i < data[0].length; ++i) {
      t.common.client.Utils.makeColumn(table, i, data[0][i], "12em");
    }
    
    String[][] disp = Arrays.copyOfRange(data, 1, data.length);
    table.setRowData(Arrays.asList(disp));
    table.setPageSize(100);
  }
}
