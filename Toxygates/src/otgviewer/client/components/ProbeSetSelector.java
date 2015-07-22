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

import java.util.ArrayList;
import java.util.List;

import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.viewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class ProbeSetSelector extends DataListenerWidget {

  private final Screen screen;

  private HorizontalPanel selector;
  private ListChooser listChooser;

  private Button btnNew;
  private Button btnEdit;

  public ProbeSetSelector(Screen screen) {
    this.screen = screen;
    makeSelector();
  }

  private void makeSelector() {
    selector = Utils.mkHorizontalPanel(true);
    selector.setStylePrimaryName("colored");
    selector.addStyleName("slightlySpaced");
    // addListener(selector);

    listChooser =
        new ListChooser(new ArrayList<StringList>(), "probes", false) {
          @Override
          protected void itemsChanged(List<String> items) {
            btnEdit.setEnabled(true);
            ProbeSetSelector.this.itemsChanged(items);
          }

          @Override
          protected void listsChanged(List<ItemList> lists) {
            ProbeSetSelector.this.listsChanged(lists);
          }

          @Override
          protected void onDefaultItemSelected() {
            super.onDefaultItemSelected();
            btnEdit.setEnabled(false);
            ProbeSetSelector.this.itemsChanged(new ArrayList<String>());
          }

        };
    screen.addListener(listChooser);

    final EditorCallback callback = new EditorCallback() {
      @Override
      public void onSaved(String title, List<String> items) {
        itemsChanged(items);
        listChooser.setSelected(title);
      }

      @Override
      public void onCanceled() {}

    };

    btnNew = new Button("New", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        new ProbeSetEditor(screen, callback).createNew();
      }
    });

    btnEdit = new Button("Edit", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        new ProbeSetEditor(screen, callback).edit(listChooser.getSelectedText());
      }
    });
    btnEdit.setEnabled(false);

    selector.add(listChooser);
    selector.add(btnNew);
    selector.add(btnEdit);
  }

  public Widget selector() {
    return selector;
  }

  protected abstract void itemsChanged(List<String> items);

  protected abstract void listsChanged(List<ItemList> lists);

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    listChooser.setLists(lists);
  }

}
