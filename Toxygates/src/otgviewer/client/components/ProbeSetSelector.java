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

package otgviewer.client.components;

import t.viewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class ProbeSetSelector extends DataListenerWidget {

  private HorizontalPanel selector;

  private ListBox listProbeset;

  public ProbeSetSelector() {
    makeSelector();
  }

  private void makeSelector() {
    selector = Utils.mkHorizontalPanel(true);
    selector.setStylePrimaryName("colored");
    selector.addStyleName("slightlySpaced");
    // addListener(selector);

    listProbeset = new ListBox();
    listProbeset.setVisibleItemCount(10);

    Button btnNew = new Button("New", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // TODO Auto-generated method stub

      }
    });

    Button btnEdit = new Button("Edit", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // TODO Auto-generated method stub

      }
    });
    
    selector.add(listProbeset);
    selector.add(btnNew);
    selector.add(btnEdit);
  }

  public Widget selector() {
    return selector;
  }

  public abstract void probeSetChanged();
  public abstract void saveActionPerformed();
  
}
