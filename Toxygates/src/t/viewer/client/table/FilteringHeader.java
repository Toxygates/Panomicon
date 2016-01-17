package t.viewer.client.table;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Header;

class FilteringHeader extends Header<SafeHtml> {
  private SafeHtml value;

  public FilteringHeader(SafeHtml value, boolean active) {
    super(new FilterCell(active));
    this.value = value;
  }

  @Override
  public SafeHtml getValue() {
    return value;
  }
}