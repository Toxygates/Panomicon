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

import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

import t.viewer.client.Utils;
import t.viewer.client.components.ImmediateValueChangeTextBox;

/**
 * Dialog box for getting a string input from the user. Can also
 * react every time the content of the text box changes.
 */
public class InputDialog extends Composite {
  public TextBox input;
  public Button submitButton, cancelButton;

  public InputDialog(String message) {
    this(message, "");
  }

  public InputDialog(String message, String initialText) {
    VerticalPanel vp = new VerticalPanel();
    vp.setWidth("100%");
    vp.add(new Label(message));
    initWidget(vp);

    input = new ImmediateValueChangeTextBox();
    input.setText(initialText);
    vp.add(input);
    input.addValueChangeHandler(event -> {
      onTextBoxValueChange(input.getValue());
    });

    submitButton = new Button("Save");
    submitButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onChange(input.getText());
      }
    });

    cancelButton = new Button("Cancel");
    cancelButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onChange(null);
      }
    });
    HorizontalPanel hp = Utils.mkHorizontalPanel(true, submitButton, cancelButton);
    hp.setWidth("100%");
    vp.add(hp);
    onTextBoxValueChange(initialText);
  }

  /**
   * Called every time the value in the text box changes
   * @param newValue the new text box value
   */
  protected void onTextBoxValueChange(String newValue) {
    
  }
  
  /**
   * Called when the user submits or cancels the dialog
   * @param result either the value in the text box, or null if the
   * dialog was canceled
   */
  protected void onChange(@Nullable String result) {

  }
}
