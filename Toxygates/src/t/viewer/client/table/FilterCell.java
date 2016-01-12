package t.viewer.client.table;

import otgviewer.client.Resources;
import t.common.client.ImageClickCell;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;

class FilterCell extends ImageClickCell.SafeHtmlImageClickCell {
  
  private static Resources resources = GWT.create(Resources.class);
  
  public FilterCell() {
    super(resources.filter(), true);
  }

  public void onClick(SafeHtml value) {
    /*
     * The filtering mechanism is not handled here, but in ExpressionTable.interceptGridClick.
     */
  }
}
