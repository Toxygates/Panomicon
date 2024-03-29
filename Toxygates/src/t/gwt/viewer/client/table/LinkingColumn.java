/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.gwt.viewer.client.table;

import com.google.gwt.cell.client.SafeHtmlCell;
import t.shared.common.SharedUtils;
import t.shared.viewer.AssociationValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class LinkingColumn<T> extends HTMLHideableColumn<T> {
  public LinkingColumn(SafeHtmlCell c, String name, boolean initState, String width,
      @Nullable StandardColumns col) {
    super(c, name, initState, width, col);
  }

  public LinkingColumn(SafeHtmlCell c, String name, StandardColumns col, TableStyle style) {
    this(c, name, style.initVisibility(col), style.initWidth(col), col);
  }

  public static final int MAX_ITEMS = 10;

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
        r.add("<div> ... (" + Math.min(values.size(), MAX_ITEMS) +
                ( values.size() > MAX_ITEMS ? "+ items" : " items") +
            ")");
      }
    }
    return r;
  }

  @Override
  protected String getHtml(T row) {
    return SharedUtils.mkString(makeLinks(getLinkableValues(row)), "");
  }

  protected Collection<AssociationValue> getLinkableValues(T data) {
    return new ArrayList<AssociationValue>();
  }

  protected abstract String formLink(String value);

}