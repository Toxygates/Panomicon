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

import otgviewer.client.DataScreen;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.viewer.client.CodeDownload;
import t.viewer.client.Utils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class GeneSetSelector extends DataListenerWidget {

  public static final String ALL_PROBES = "All probes";

  private final DataScreen screen;

  private HorizontalPanel selector;
  private ListChooser geneSets;

  private Button btnNew;
  private Button btnEdit;

  public GeneSetSelector(DataScreen screen) {
    this.screen = screen;
    makeSelector();
  }

  private void makeSelector() {
    selector = Utils.mkHorizontalPanel(true);
    selector.setHeight(DataScreen.STANDARD_TOOL_HEIGHT + "px");
    selector.setStylePrimaryName("colored");
    selector.addStyleName("slightlySpaced");

    geneSets = new ListChooser("probes", false, ALL_PROBES) {
      @Override
      protected void itemsChanged(List<String> items) {
        super.itemsChanged(items);

        String[] itemsArray = items.toArray(new String[0]);
        logger.info("Items: " + itemsArray);
        screen.geneSetChanged(new StringList("probes", getSelectedText(), itemsArray));
        screen.probesChanged(itemsArray);
        
        GeneSetSelector.this.itemsChanged(items);
      }
    };
    addListener(geneSets);

    btnNew = new Button("New", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
       GWT.runAsync(new CodeDownload(logger) {
         public void onSuccess() {
           geneSetEditorNew();
         }
       });
      }
    });

    btnEdit = new Button("Edit", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        GWT.runAsync(new CodeDownload(logger) {
          public void onSuccess() {
            geneSetEditorEdit();
          }
        });
      }
    });
    btnEdit.setEnabled(false);

    selector.add(new Label("GeneSet:"));
    selector.add(geneSets);
    selector.add(btnNew);
    selector.add(btnEdit);
  }

  private void geneSetEditorNew() {
    geneSetEditor().createNew(screen.displayedAtomicProbes());        
  }
  
  private void geneSetEditorEdit() {
    geneSetEditor().edit(geneSets.getSelectedText());
  }
  
  private GeneSetEditor geneSetEditor() {
    GeneSetEditor gse = screen.factory().geneSetEditor(screen);
    gse.addSaveActionHandler(new SaveActionHandler() {
      @Override
      public void onSaved(String title, List<String> items) {
        String[] itemsArray = items.toArray(new String[0]);
        screen.geneSetChanged(new StringList("probes", title, itemsArray));
        screen.probesChanged(itemsArray);
        screen.updateProbes();
      }
      @Override
      public void onCanceled() {}
    });
    addListener(gse);
    return gse;
  }

  public Widget selector() {
    return selector;
  }
  
  /**
   * To be overridden by subclasses/users. Called when the user has triggered a change.
   */
  public void itemsChanged(List<String> items) {};

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    geneSets.setLists(lists);
  }  

  @Override
  public void geneSetChanged(ItemList geneSet) {
    super.geneSetChanged(geneSet);
    
    int selected = 0;
    if (geneSet != null) {
      selected = geneSets.trySelect(geneSet.name());
    }
    
    btnEdit.setEnabled(selected > 0);
  }

  public boolean isDefaultItemSelected() {
    return (geneSets.getSelectedIndex() == 0);
  }
}
