/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client;

import com.google.gwt.cell.client.*;
import com.google.gwt.dom.client.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.*;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * A clickable cell that displays an image and optionally text. If text is displayed, it will appear
 * before the image.
 */
abstract public class ImageClickCell<T> extends AbstractCell<T> {
  private ImageResource image;
  private boolean displayText;

  public static final String CLICK_ELEMENT_ID = "imageClickCell";
  
  public ImageClickCell(ImageResource image, boolean displayText) {
    super("click");
    this.image = image;
    this.displayText = displayText;
  }

  public void render(Cell.Context context, T data, SafeHtmlBuilder sb) {
    if (displayText) {
      appendText(data, sb);
    }       
    sb.append(SafeHtmlUtils.fromTrustedString("<span id=\"" + CLICK_ELEMENT_ID + 
        "\" style=\"margin:5px\">"
        + AbstractImagePrototype.create(image).getHTML() + "</span>"));
  }

  abstract protected void appendText(T text, SafeHtmlBuilder sb);

  @Override
  public void onBrowserEvent(Context context, Element parent, T value, NativeEvent event,
      ValueUpdater<T> valueUpdater) {
    if ("click".equals(event.getType())) {
      EventTarget et = event.getEventTarget();
      if (Element.is(et)) {
        Element e = et.cast();
        String target = e.getString();

        // TODO this is a bit hacky - is there a better way?
        boolean targetWasImage = (target.startsWith("<img") | target.startsWith("<IMG"));
        if (targetWasImage) {
          onClick(value);
          return;
        }
      }
    }
    super.onBrowserEvent(context, parent, value, event, valueUpdater);
  }

  abstract public void onClick(T value);

  abstract public static class StringImageClickCell extends ImageClickCell<String> {
    public StringImageClickCell(ImageResource image, boolean displayText) {
      super(image, displayText);
    }

    protected void appendText(String text, SafeHtmlBuilder sb) {
      sb.appendHtmlConstant(text);
    }
  }

  abstract public static class SafeHtmlImageClickCell extends ImageClickCell<SafeHtml> {
    public SafeHtmlImageClickCell(ImageResource image, boolean displayText) {
      super(image, displayText);
    }

    protected void appendText(SafeHtml text, SafeHtmlBuilder sb) {
      sb.append(text);
    }
  }
}
