package t.viewer.client.table;

import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.cellview.client.DataGrid;

/**
 * A DataGrid with publicly accessible getTableHeadElement method 
 */
public class PublicHeaderDataGrid<T> extends DataGrid<T> {
  public PublicHeaderDataGrid(int pageSize, Resources resources) {
    super(pageSize, resources);
  }
  
  @Override 
  public TableSectionElement getTableHeadElement() {
    return super.getTableHeadElement();
  }
}
