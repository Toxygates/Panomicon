package t.viewer.client.table;

import javax.annotation.Nullable;

import t.viewer.client.Utils;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

/**
 * A text column that can potentially display tooltips for each cell.
 * @param <R>
 */
public abstract class TooltipColumn<R> extends Column<R, String> {

  public TooltipColumn(Cell<String> cell) {
    super(cell);   
  }

  protected static Utils.Templates TEMPLATES = GWT.create(Utils.Templates.class);

  protected @Nullable String getTooltip(R item) {
    return null;
  }

  @Override
  public void render(final Context context, final R object, final SafeHtmlBuilder sb) {
    if (object != null) {
      htmlBeforeContent(sb, object);
      super.render(context, object, sb);
      htmlAfterContent(sb, object);
    } else {
      super.render(context, object, sb);
    }
  }

  protected void htmlBeforeContent(SafeHtmlBuilder sb, R object) {
    String tooltip = getTooltip(object);
    sb.append(TEMPLATES.startToolTip(tooltip));
  }
  
  protected void htmlAfterContent(SafeHtmlBuilder sb, R object) {
    String tooltip = getTooltip(object);
    if (tooltip != null) {
      sb.append(TEMPLATES.endToolTip());
    }
  }
  
}