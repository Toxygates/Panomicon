/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.table;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

import t.viewer.client.Utils;

/**
 * A text column that can potentially display tooltips for each cell.
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
    if (tooltip != null) {
      sb.append(TEMPLATES.startToolTip(tooltip));
    }
  }
  
  protected void htmlAfterContent(SafeHtmlBuilder sb, R object) {
    String tooltip = getTooltip(object);
    if (tooltip != null) {
      sb.append(TEMPLATES.endToolTip());
    }
  }
  
}
