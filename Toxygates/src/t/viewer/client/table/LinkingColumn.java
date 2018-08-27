package t.viewer.client.table;

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.SafeHtmlCell;

import t.common.shared.SharedUtils;
import t.viewer.shared.AssociationValue;

public abstract class LinkingColumn<T> extends HTMLHideableColumn<T> {
  public LinkingColumn(SafeHtmlCell c, String name, boolean initState, String width,
      @Nullable StandardColumns col) {
    super(c, name, initState, width, col);
  }

  public LinkingColumn(SafeHtmlCell c, String name, StandardColumns col, TableStyle style) {
    this(c, name, style.initVisibility(col), style.initWidth(col), col);
  }

  final int MAX_ITEMS = 10;

  // Note: might move this method down or parameterise AssociationValue,
  // use an interface etc
  protected List<String> makeLinks(Collection<AssociationValue> values) {
    List<String> r = new ArrayList<String>();
    int i = 0;
    for (AssociationValue v : values) {
      i += 1;
      if (i <= MAX_ITEMS) {
        String l = formLink(v.formalIdentifier());
        if (l != null) {
          r.add("<div class=\"associationValue\" title=\"" + v.tooltip()
              + "\"><a target=\"_TGassoc\" href=\"" + l + "\">" + v.title() + "</a></div>");
        } else {
          r.add("<div class=\"associationValue\" title=\"" + v.tooltip() + "\">" + v.title()
              + "</div>"); // no link
        }
      }
      if (i == MAX_ITEMS + 1) {
        r.add("<div> ... (" + values.size() + " items)");
      }
    }
    return r;
  }

  @Override
  protected String getHtml(T er) {
    return SharedUtils.mkString(makeLinks(getLinkableValues(er)), "");
  }

  protected Collection<AssociationValue> getLinkableValues(T er) {
    return new ArrayList<AssociationValue>();
  }

  protected abstract String formLink(String value);

}