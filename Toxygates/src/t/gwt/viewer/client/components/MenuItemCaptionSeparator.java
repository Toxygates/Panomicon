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

package t.gwt.viewer.client.components;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.MenuItemSeparator;

/*
 * Hack GWT-MenuItemSeparator to indicate caption upon the separator
 */
public class MenuItemCaptionSeparator extends MenuItemSeparator {
  public MenuItemCaptionSeparator(String caption) {
    super();
    getElement().removeAllChildren();

    Element div = DOM.createDiv();
    div.setInnerHTML(caption.replaceAll("\n", "<br>"));
    DOM.appendChild(getElement(), div);
    setStyleName(div, "menuSeparatorCaption");

    div = DOM.createDiv();
    DOM.appendChild(getElement(), div);
    setStyleName(div, "menuSeparatorInner");
  }
}