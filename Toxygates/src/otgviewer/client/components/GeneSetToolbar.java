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

package otgviewer.client.components;

import java.util.List;

import otgviewer.client.DataScreen;
import t.common.shared.ClusteringList;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.clustering.ProbeClustering;
import t.viewer.client.CodeDownload;
import t.viewer.client.Utils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class GeneSetToolbar extends DataListenerWidget {

  public static final String ALL_PROBES = "All Probes";
  private static final String PATH_SEPARATOR = " / ";

  private final DataScreen screen;

  private HorizontalPanel selector;
  private Label lblSelected;

  private Button btnNew;
  private Button btnEdit;

  public GeneSetToolbar(DataScreen screen) {
    this.screen = screen;
    makeTool();
  }

  private void makeTool() {
    selector = Utils.mkHorizontalPanel(true);
    selector.setHeight(DataScreen.STANDARD_TOOL_HEIGHT + "px");
    selector.setStylePrimaryName("colored");
    selector.addStyleName("slightlySpaced");

    lblSelected = new Label();

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

    selector.add(lblSelected);
    selector.add(btnNew);
    selector.add(btnEdit);
  }

  private void geneSetEditorNew() {    
    geneSetEditor().createNew(screen.displayedAtomicProbes());
  }

  private void geneSetEditorEdit() {
    geneSetEditor().edit(screen.chosenGeneSet.name());
  }

  private GeneSetEditor geneSetEditor() {
    GeneSetEditor gse = screen.factory().geneSetEditor(screen);
    gse.addSaveActionHandler(new SaveActionHandler() {
      @Override
      public void onSaved(String title, List<String> items) {
        String[] itemsArray = items.toArray(new String[0]);
        screen.geneSetChanged(new StringList(StringList.PROBES_LIST_TYPE, 
            title, itemsArray));
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
  public void geneSetChanged(ItemList geneSet) {
    super.geneSetChanged(geneSet);

    btnEdit.setEnabled(false);

    if (geneSet == null) {
      lblSelected.setText(ALL_PROBES);
      return;
    }

    String path = null;
    if (geneSet.type().equals("probes")) {
      path = geneSet.name();
      btnEdit.setEnabled(true);
    } else if (geneSet.type().equals(ClusteringList.USER_CLUSTERING_TYPE)) {
      ClusteringList cl = (ClusteringList) geneSet;
      path = geneSet.name() + PATH_SEPARATOR + cl.items()[0].name();
    } else if (geneSet.type().equals(ProbeClustering.PROBE_CLUSTERING_TYPE)) {
      path = geneSet.name().replaceAll("\\#\\#\\#", PATH_SEPARATOR);
      if (path.endsWith(PATH_SEPARATOR)) {
        path = path.substring(0, path.lastIndexOf(PATH_SEPARATOR));
      }
    } else {
      path = geneSet.name();
    }
    lblSelected.setText(path);

  }

}
