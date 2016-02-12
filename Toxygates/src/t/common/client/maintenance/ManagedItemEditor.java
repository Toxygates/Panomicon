/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client.maintenance;

import static t.common.client.Utils.makeButtons;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import t.common.client.Command;
import t.common.shared.ManagedItem;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public abstract class ManagedItemEditor extends Composite {

  protected VerticalPanel vp;
  /**
   * If true, we are adding a new item. If false, we are editing an existing item.
   */
  protected final boolean addNew;

  protected final TextBox idText;
  protected final TextArea commentArea;

  public ManagedItemEditor(@Nullable ManagedItem i, boolean addNew) {
    vp = new VerticalPanel();
    this.addNew = addNew;
    initWidget(vp);

    idText = addLabelledTextBox("ID");
    idText.setEnabled(addNew);
    
    commentArea = addTextArea("Private comments");
    if (i != null) {
      idText.setValue(i.getTitle());
      commentArea.setValue(i.getComment());
    }          
  }

  protected TextArea addTextArea(String label) {
    vp.add(new Label(label));
    TextArea r = new TextArea();
    r.setSize("400px", "50px");
    vp.add(r);
    return r;
  }
  
  protected TextBox addLabelledTextBox(String label) {
    Label l = new Label(label);
    vp.add(l);
    TextBox text = new TextBox();
    vp.add(text);
    return text;
  }

  protected void addCommands() {
    List<Command> cmds = new ArrayList<Command>();
    Command c = new Command("OK") {
      @Override
      public void run() {
        triggerEdit();
      }
    };
    cmds.add(c);

    c = new Command("Cancel") {
      @Override
      public void run() {
        onFinishOrAbort();
        onAbort();
      }

    };
    cmds.add(c);

    Widget btns = makeButtons(cmds);
    vp.add(btns);
  }

  protected abstract void triggerEdit();

  protected AsyncCallback<Void> editCallback() {
    return new AsyncCallback<Void>() {
      @Override
      public void onSuccess(Void result) {
        Window.alert("Operation successful");
        onFinishOrAbort();
        onFinish();
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Operation failed: " + caught.getMessage());
        onFinishOrAbort();
        onAbort();
      }
    };
  }

  /**
   * Called when the edit operation was finished or cancelled
   */
  protected void onFinishOrAbort() {}

  /**
   * Called when the edit operation has successfully finished.
   */
  protected void onFinish() {}

  /**
   * Called when the edit operation was cancelled (by the user or due to an error)
   */
  protected void onAbort() {}
}
