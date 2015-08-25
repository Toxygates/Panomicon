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

import java.util.List;

import t.common.shared.ItemList;
import t.viewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class GeneSetSelector extends DataListenerWidget {

  private static final String ALL_PROBES = "All probes";

  private final Screen screen;

  private HorizontalPanel selector;
  private ListChooser geneSets;

  private Button btnNew;
  private Button btnEdit;

  public GeneSetSelector(Screen screen) {
    this.screen = screen;
    makeSelector();
  }

  private void makeSelector() {
    selector = Utils.mkHorizontalPanel(true);
    selector.setStylePrimaryName("colored");
    selector.addStyleName("slightlySpaced");

    geneSets = new ListChooser("probes", false, ALL_PROBES) {
      @Override
      protected void itemsChanged(List<String> items) {
        super.itemsChanged(items);

        logger.info("Items: " + items.toArray(new String[0]));
        screen.probesChanged(items.toArray(new String[0]));
        screen.geneSetChanged(getSelectedText());
      }
    };
    addListener(geneSets);

    btnNew = new Button("New", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        geneSetEditor().createNew();
      }
    });

    btnEdit = new Button("Edit", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        geneSetEditor().edit(geneSets.getSelectedText());
      }
    });
    btnEdit.setEnabled(false);

    selector.add(new Label("GeneSet:"));
    selector.add(geneSets);
    selector.add(btnNew);
    selector.add(btnEdit);
  }

  private GeneSetEditor geneSetEditor() {
    return new GeneSetEditor(screen) {
      @Override
      protected void onSaved(String title, List<String> items) {
        super.onSaved(title, items);
        screen.probesChanged(items.toArray(new String[0]));
        screen.geneSetChanged(title);
      }
    };
  }

  public Widget selector() {
    return selector;
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    geneSets.setLists(lists);
  }

  @Override
  public void geneSetChanged(String geneSet) {
    super.geneSetChanged(geneSet);
    int selected = geneSets.trySelect(geneSet);
    btnEdit.setEnabled(selected > 0);
  }

  public boolean isDefaultItemSelected() {
    return (geneSets.getSelectedIndex() == 0);
  }
}
