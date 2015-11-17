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

package otgviewer.client.targetmine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.dialog.InteractionDialog;
import otgviewer.client.dialog.TargetMineEnrichDialog;
import otgviewer.client.dialog.TargetMineSyncDialog;
import otgviewer.shared.targetmine.EnrichmentParams;
import t.common.client.components.StringArrayTable;
import t.common.shared.ItemList;
import t.common.shared.SharedUtils;
import t.common.shared.StringList;
import t.viewer.client.dialog.DialogPosition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * Client side helper for TargetMine data import/export.
 */
public class TargetMineData {

  final Screen parent;
  final TargetmineServiceAsync tmService = (TargetmineServiceAsync) GWT
      .create(TargetmineService.class);
  final String url;

  private Logger logger = SharedUtils.getLogger("targetmine");

  public TargetMineData(Screen parent) {
    this.parent = parent;
    url = parent.appInfo().targetmineURL();
  }

  DialogBox dialog;

  public void importLists(final boolean asProbes) {
    InteractionDialog ui = new TargetMineSyncDialog(parent, url, "Import") {
      @Override
      protected void userProceed(String user, String pass, boolean replace) {
        super.userProceed();
        doImport(user, pass, asProbes, replace);
      }

    };
    ui.display("TargetMine import details", DialogPosition.Center);
  }

  public void doImport(final String user, final String pass, final boolean asProbes,
      final boolean replace) {
    tmService.importTargetmineLists(user, pass, asProbes, new PendingAsyncCallback<StringList[]>(
        parent, "Unable to import lists from TargetMine. Check your username and password. "
            + "There may also be a server error.") {
      public void handleSuccess(StringList[] data) {
        parent.itemListsChanged(mergeLists(parent.chosenItemLists, Arrays.asList(data), replace));
        parent.storeItemLists(parent.getParser());
      }
    });
  }

  public void exportLists() {
    InteractionDialog ui = new TargetMineSyncDialog(parent, url, "Export") {
      @Override
      protected void userProceed(String user, String pass, boolean replace) {
        super.userProceed();
        doExport(user, pass, 
            StringList.pickProbeLists(parent.chosenItemLists, null), replace);
      }

    };
    ui.display("TargetMine export details", DialogPosition.Center);
  }

  // Could factor out code that is shared with doImport, but the two dialogs may diverge
  // further in the future.
  public void doExport(final String user, final String pass, final List<StringList> lists,
      final boolean replace) {
    tmService.exportTargetmineLists(user, pass, lists.toArray(new StringList[0]), replace,
        new PendingAsyncCallback<Void>(parent,
            "Unable to export lists to TargetMine. Check your username and password. "
                + "There may also be a server error.") {
          public void handleSuccess(Void v) {
            Window.alert("The lists were successfully exported.");
          }
        });
  }

  public void enrich() {
    InteractionDialog ui = new TargetMineEnrichDialog(parent, url, "Enrich") {
      @Override
      protected void userProceed(String user, String pass, boolean replace) {
        super.userProceed();
        String chosen = parent.chosenGeneSet;
        if (chosen != null && !chosen.equals("")) {          
          doEnrich(user, pass, 
              StringList.pickProbeLists(parent.chosenItemLists, chosen).get(0),
              getParams());
        } else {
          Window.alert("Please define and select a probe list first.");
        }        
      }
    };
    ui.display("TargetMine enrichment", DialogPosition.Center);
  }

  public void doEnrich(final String user, final String pass, StringList list,
      EnrichmentParams params) {
    tmService.enrichment(user, pass, list, params, new PendingAsyncCallback<String[][]>(parent,
        "Unable to perform enrichment analysis. Check your username and password. "
            + "There may also be a server error.") {
      public void handleSuccess(String[][] result) {
        StringArrayTable.displayDialog(result, "Enrichment results", 800, 600);            
      }
    });
  }
  
  public void multiEnrich(final StringList[] lists) {
    InteractionDialog ui = new TargetMineEnrichDialog(parent, url, "Cluster enrichment") {
      @Override
      protected void userProceed(String user, String pass, boolean replace) {
        super.userProceed();
        doEnrich(user, pass, lists, getParams());
      }
    };
    ui.display("TargetMine cluster enrichment", DialogPosition.Center);
  }
  
  public void doEnrich(final String user, final String pass, StringList[] lists,
      EnrichmentParams params) {
    tmService.multiEnrichment(user, pass, lists, params,
         new PendingAsyncCallback<String[][][]>(parent,
        "Unable to perform enrichment analysis. Check your username and password. "
            + "There may also be a server error.") {
      public void handleSuccess(String[][][] result) {
        List<String[]> best = new ArrayList<String[]>();
        best.add(result[0][0]); //Headers
        for (String[][] clust: result) {
          if (clust.length < 2) {
            //TODO don't hardcode length here
            String[] res = new String[] { "(No result)", "", "", "" };
            best.add(res);
          } else {
            best.add(clust[1]);
          }
        }
        StringArrayTable.displayDialog(best.toArray(new String[0][0]), "Best enrichment results", 800, 400);            
      }
    });
  }

  List<ItemList> mergeLists(List<? extends ItemList> into, List<? extends ItemList> from,
      boolean replace) {
    Map<String, ItemList> allLists = new HashMap<String, ItemList>();
    int addedLists = 0;
    int addedItems = 0;
    int nonImported = 0;
    int hadGenesForSpecies = 0;

    for (ItemList l : into) {
      allLists.put(l.name(), l);
    }

    for (ItemList l : from) {
      if (replace || !allLists.containsKey(l.name())) {
        allLists.put(l.name(), l);
        addedLists++;
        addedItems += l.size();
        String comment = ((StringList) l).getComment();
        Integer genesForSpecies = Integer.parseInt(comment);

        logger.info("Import list " + l.name() + " size " + l.size() + " comment " + comment);
        if (genesForSpecies > 0) {
          hadGenesForSpecies++;
        }
      } else if (allLists.containsKey(l.name())) {
        logger.info("Do not import list " + l.name());
        nonImported += 1;
      }
    }

    String msg =
        addedLists + " lists with " + addedItems + " items were successfully imported.\nOf these, ";
    if (hadGenesForSpecies == 0) {
      msg += "no list ";
    } else {
      msg += hadGenesForSpecies + " list(s) ";
    }
    msg += " contained genes for " + parent.chosenSampleClass.get("organism") + ".";

    if (nonImported > 0) {
      msg = msg + "\n" + nonImported + " lists with identical names were not imported.";
    }
    Window.alert(msg);
    return new ArrayList<ItemList>(allLists.values());
  }
}
