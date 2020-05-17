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

package t.viewer.client.screen.data;

import java.util.Collection;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.viewer.client.components.OTGScreen;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.shared.mirna.MirnaSource;

public class MirnaSourceDialog extends InteractionDialog {
  MirnaSourceSelector selector;
  VerticalPanel vp;
  Delegate delegate;
  
  public interface Delegate {
    void mirnaSourceDialogMirnaSourcesChanged(MirnaSource[] mirnaSources);
  }

  public MirnaSourceDialog(OTGScreen parent, Delegate delegate,
                           MirnaSource[] availableSources,
                           List<MirnaSource> value) {
    super(parent);
    this.delegate = delegate;
    this.selector = new MirnaSourceSelector(availableSources, value);
    vp = Utils.mkVerticalPanel(true);
    vp.add(selector);
    
    Button okButton = new Button("OK", new ClickHandler() {
      @Override
      public void onClick(ClickEvent e) {
        userProceed();        
      }      
    });
    
    Button cancelButton = new Button("Cancel", new ClickHandler() {
      @Override
      public void onClick(ClickEvent e) {
        userCancel();        
      }      
    });
    
    Panel p = Utils.mkHorizontalPanel(true, okButton, cancelButton);
    vp.add(p);
  }
  
  @Override
  protected Widget content() {
    return vp;
  }
  
  @Override
  protected void userProceed() {
    try {
      Collection<MirnaSource> selection = selector.getSelection();

      delegate.mirnaSourceDialogMirnaSourcesChanged(selection.toArray(new MirnaSource[0]));
      MirnaSourceDialog.super.userProceed();
    } catch (NumberFormatException e) {
      Window.alert("Please enter a numerical value as the score cutoff.");
    }
  }
}
