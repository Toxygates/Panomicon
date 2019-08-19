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

package t.viewer.client.components;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Custom TextBox that fires ValueChangeEvents on text cut/paste, and after
 * every keypress (regular TextBoxes won't fire a ValueChangeEvent until after
 * losing input focus).
 */
public class ImmediateValueChangeTextBox extends TextBox {
  public ImmediateValueChangeTextBox() {
    super();
    addCutHandler(this.getElement());
    addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        ValueChangeEvent.fire(ImmediateValueChangeTextBox.this, getText());
      }
    });
  }

  private void fireDeferredValueChangeEvent() {
    Scheduler.get().scheduleDeferred(() -> {
      ValueChangeEvent.fire(ImmediateValueChangeTextBox.this, getText());
    });
  }

  @SuppressWarnings("deprecation") // suppress deprecation of Element
  private native void addCutHandler(com.google.gwt.user.client.Element element) /*-{
    var self = this;
    element.oncut = element.onpaste = function(e) {
      self.@t.viewer.client.components.ImmediateValueChangeTextBox::fireDeferredValueChangeEvent()();
    }
  }-*/;
}