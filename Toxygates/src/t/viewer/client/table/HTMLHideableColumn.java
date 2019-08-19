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

package t.viewer.client.table;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * A hideable column that displays SafeHtml
 */
public abstract class HTMLHideableColumn<T> extends HideableColumn<T, SafeHtml> {

  protected String _width;
  protected String _name;
  protected SafeHtmlCell _c;

  /**
   * The standard column represented here, if any
   */
  protected @Nullable StandardColumns col;

  public HTMLHideableColumn(SafeHtmlCell c, String name, boolean initState, String width,
      @Nullable StandardColumns col) {
    super(c, initState, col);
    this._c = c;
    _name = name;
    _width = width;
    _columnInfo = new ColumnInfo(name, width, false);
  }

  public HTMLHideableColumn(SafeHtmlCell c, String name,
      StandardColumns col, TableStyle style) {
    this(c, name, style.initVisibility(col), style.initWidth(col), col);
  }

  @Override
  public SafeHtml getValue(T er) {
    SafeHtmlBuilder build = new SafeHtmlBuilder();
    build.appendHtmlConstant(getHtml(er));
    return build.toSafeHtml();
  }

  protected abstract String getHtml(T er);
}
