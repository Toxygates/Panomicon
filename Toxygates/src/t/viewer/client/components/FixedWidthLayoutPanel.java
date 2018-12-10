/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.components;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;

/**
 * A layout panel that tries to keep a single widget at a fixed width, centered, filling all
 * vertical space.
 *
 */
public class FixedWidthLayoutPanel extends LayoutPanel {

  private int fixedWidth;
  private Widget inner;

  public FixedWidthLayoutPanel(Widget inner, int fixedWidth, int margin) {
    add(inner);
    this.inner = inner;
    setWidgetTopBottom(inner, margin, Unit.PX, margin, Unit.PX);
    this.fixedWidth = fixedWidth;
    adjustWidth();
  }

  private void adjustWidth() {
    int w = getOffsetWidth();
    int dist = w / 2 - fixedWidth / 2;
    if (dist < 0) {
      dist = 0;
    }
    setWidgetLeftRight(inner, dist, Unit.PX, dist, Unit.PX);
  }

  @Override
  public void onResize() {
    super.onResize();
    adjustWidth();
    if (inner instanceof RequiresResize) {
      ((RequiresResize) inner).onResize();
    }
  }

}
