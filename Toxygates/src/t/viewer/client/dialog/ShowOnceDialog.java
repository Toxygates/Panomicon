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

import com.google.gwt.user.client.ui.*;

import t.viewer.client.Utils;

/**
 * A message dialog that lets the user suppress future occurrences.
 */
public class ShowOnceDialog {
  boolean shouldShow = true;

  DialogBox db = new DialogBox();
  VerticalPanel vp = Utils.mkVerticalPanel(true);
  Label message = new Label();
  CheckBox check = new CheckBox("Do not show this message again");
  Button b = new Button("Close");

  public ShowOnceDialog() {
    vp.add(message);
    message.setWordWrap(true);
    message.addStyleName("popupMessage");
    vp.add(check);
    vp.add(b);
    db.add(vp);
    b.addClickHandler(e -> close());
  }

  public void showIfDesired(String message) {
    if (shouldShow) {
      this.message.setText(message);
      Utils.displayInCenter(db);
    }
  }

  public void close() {
    this.shouldShow = !check.getValue();
    db.hide();
  }
}
