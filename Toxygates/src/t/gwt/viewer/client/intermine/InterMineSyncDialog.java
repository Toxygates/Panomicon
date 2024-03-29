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

package t.gwt.viewer.client.intermine;

import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.gwt.viewer.client.Utils;
import t.gwt.viewer.client.components.InputGrid;
import t.gwt.viewer.client.screen.Screen;
import t.gwt.viewer.client.dialog.InteractionDialog;
import t.shared.viewer.intermine.IntermineInstance;

abstract public class InterMineSyncDialog extends InteractionDialog {

  private CheckBox replaceCheck = new CheckBox("Replace lists with identical names");
  private InputGrid ig;
  private String action;

  private boolean withPassword, withReplace;
  
  // Task: it would be better to store these on a per-instance basis
  private static String account, password;
  private @Nullable IntermineInstance instance;
  protected @Nullable InstanceSelector selector;
  private @Nullable String message;
  
  /**
   * 
   * @param parent
   * @param action
   * @param withPassword
   * @param withReplace
   * @param instance must not be null if withPassword is true
   */
  public InterMineSyncDialog(Screen parent, String action,
      boolean withPassword, boolean withReplace,
      @Nullable InstanceSelector selector,
      @Nullable IntermineInstance instance) {
    super(parent);
    this.action = action;
    this.instance = instance;
    this.withPassword = withPassword;
    this.withReplace = withReplace;
    this.selector = selector;
    setup();
  }
  
  public InterMineSyncDialog(Screen parent, String action,
      boolean withPassword, boolean withReplace,
      @Nullable IntermineInstance preferredInstance, @Nullable String message) {
    this(parent, action, withPassword, withReplace, null,
        preferredInstance);
    this.selector = new InstanceSelector(parent.appInfo());
    if (preferredInstance != null) {
      selector.setSelected(preferredInstance);
    }
    this.message = message;
    setup();
  }
  
  private void setup() {
    if (selector != null) {
      selector.listBox().addChangeHandler(new ChangeHandler() {        
        @Override
        public void onChange(ChangeEvent event) {
          instanceChanged(selector.value());
        }
      });
    }
  }

  @Override
  protected Widget content() {
    VerticalPanel vp = new VerticalPanel();
    vp.setWidth("600px");
    
    if (selector != null) {
      vp.add(new Label("Data warehouse:"));
      vp.add(selector);
    }

    Widget custom = customUI();
    if (custom != null) {
      vp.add(custom);
    }

    if (withPassword) {
      addPasswordUI(vp);    
    }

    if (withReplace) {
      vp.add(replaceCheck);
    }

    if (message != null) {
      Label l = new Label();
      l.setWordWrap(true);
      l.setText(message);
      vp.add(l);
    }

    Button b = new Button(action);
    b.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        IntermineInstance instance = 
            selector != null ? selector.value() : InterMineSyncDialog.this.instance;
            
        if (withPassword && 
            (ig.getValue(0).trim().equals("") || ig.getValue(1).trim().equals(""))
            ) {
          Window.alert("Please enter both your account name and password.");
        } else if (withPassword) {
          account = ig.getValue(0);
          password = ig.getValue(1);
          userProceed(instance, ig.getValue(0), ig.getValue(1), replaceCheck.getValue());
        } else {
          userProceed(instance, null, null, replaceCheck.getValue());
        }
      }
    });

    Button b2 = new Button("Cancel", cancelHandler());

    HorizontalPanel hp = Utils.mkHorizontalPanel(true, b, b2);
    vp.add(hp);
    return vp;
  }
  
  protected void instanceChanged(IntermineInstance instance) {
    if (passwordLabel != null) {
      passwordLabel.setText("You must have a " + instance.title() + " account in order to use "
          + "this function. If you do not have one, you may create one at " + instance.webURL()
          + ".");
    }
  }
  
  Label passwordLabel;
  
  protected void addPasswordUI(VerticalPanel vp) {
    passwordLabel = new Label();
    passwordLabel.setWordWrap(true);
    instanceChanged(instance);    
    vp.add(passwordLabel);
    
    ig = new InputGrid("Account name (e-mail address)", "Password") {
      @Override
      protected TextBox initTextBox(int i) {
        if (i == 1) {
          return new PasswordTextBox();
        } else {
          return super.initTextBox(i);
        }
      }
    };
    ig.setValue(0, account);
    ig.setValue(1, password);

    vp.add(ig);
  }
  
  protected @Nullable Widget customUI() {
    return null;
  }

  abstract protected void userProceed(IntermineInstance instance, 
      String user, String pass, boolean replace);

}
