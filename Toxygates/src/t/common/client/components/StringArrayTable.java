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
//    table.setPageSize(100);
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
