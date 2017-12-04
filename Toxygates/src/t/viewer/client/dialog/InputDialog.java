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

package t.viewer.client.dialog;

import javax.annotation.Nullable;

import t.viewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

public class InputDialog extends Composite {

  public InputDialog(String message) {
    VerticalPanel vp = new VerticalPanel();
    vp.setWidth("100%");
    vp.add(new Label(message));
    initWidget(vp);

    final TextBox input = new TextBox();
    vp.add(input);

    Button b = new Button("OK");
    b.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        onChange(input.getText());

      }
    });

    Button b2 = new Button("Cancel");
    b2.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        onChange(null);
      }
    });
    HorizontalPanel hp = Utils.mkHorizontalPanel(true, b, b2);
    hp.setWidth("100%");
    vp.add(hp);
  }

  protected void onChange(@Nullable String result) {

  }

}
