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

package t.viewer.client.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

import t.viewer.client.Utils;
import t.viewer.client.screen.Screen;

abstract public class InteractionDialog {

  protected DialogBox db;
  protected final Screen parent;

  public InteractionDialog(Screen parent) {
    this.parent = parent;
  }

  abstract protected Widget content();

  protected void userProceed() {
    db.setVisible(false);
  }

  protected void userCancel() {
    db.setVisible(false);
  }

  public void display(String caption, DialogPosition pos) {
    db = Utils.displayInPopup(caption, content(), pos);
  }

  protected ClickHandler cancelHandler() {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        userCancel();
      }
    };
  }
}
