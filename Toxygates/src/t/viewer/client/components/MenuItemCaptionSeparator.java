package t.viewer.client.components;

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