/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.viewer.client.intermine;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;

import otgviewer.client.StringListsStoreHelper;
import otgviewer.client.components.ImportingScreen;
import otgviewer.client.components.PendingAsyncCallback;
import t.clustering.shared.ClusteringList;
import t.common.client.components.StringArrayTable;
import t.common.shared.SharedUtils;
import t.viewer.client.Analytics;
import t.viewer.client.dialog.*;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
import t.viewer.shared.intermine.EnrichmentParams;
import t.viewer.shared.intermine.IntermineInstance;

/**
 * Client side helper for InterMine data import/export.
 */
public class InterMineData {

  final ImportingScreen parent;
  final IntermineServiceAsync tmService = (IntermineServiceAsync) GWT
      .create(IntermineService.class);

  private Logger logger = SharedUtils.getLogger("intermine");
  private @Nullable IntermineInstance preferredInstance;

  public InterMineData(ImportingScreen parent, @Nullable IntermineInstance preferredInstance) {
    this.parent = parent;
    this.preferredInstance = preferredInstance;
  }

  DialogBox dialog;

  /**
   * PreferredInstance must not be null
   */
  public void importLists(final boolean asProbes) {
    InteractionDialog ui =
        new InterMineSyncDialog(parent, "Import", true, true, preferredInstance) {
          @Override
          protected void userProceed(IntermineInstance instance, String user, String pass,
              boolean replace) {
            super.userProceed();
            doImport(instance, user, pass, asProbes, replace);
          }

        };
    ui.display("InterMine import details", DialogPosition.Center);
  }

  private void doImport(final IntermineInstance instance, final String user, final String pass,
      final boolean asProbes, final boolean replace) {
    tmService.importLists(instance, user, pass, asProbes, new PendingAsyncCallback<StringList[]>(
        parent, "Unable to import lists from " + instance.title()) {
      @Override
      public void handleSuccess(StringList[] data) {
        Collection<ItemList> rebuild =
            StringListsStoreHelper.rebuildLists(logger, Arrays.asList(data));
        List<StringList> normal = StringList.pickProbeLists(rebuild, null);
        List<ClusteringList> clustering = ClusteringList.pickUserClusteringLists(rebuild, null);

        // TODO revise pop-up message handling for this process
        parent.intermineImport(mergeLists(parent.itemLists(), normal, replace, "lists"),
            mergeLists(parent.clusteringList(), clustering, replace, "clusters"));
      }
    });
  }

  /**
   * PreferredInstance must not be null
   */
  public void exportLists() {
    final ImportingScreen importingParent = parent;
    InteractionDialog ui =
        new InterMineSyncDialog(parent, "Export", true, true, preferredInstance) {
          @Override
          protected void userProceed(IntermineInstance instance, String user, String pass,
              boolean replace) {
            super.userProceed();
            doExport(instance, user, pass, 
                StringListsStoreHelper.compileLists(importingParent.itemLists(), importingParent.clusteringList()),
                replace);
          }

        };
    ui.display("InterMine export details", DialogPosition.Center);
  }

  // Could factor out code that is shared with doImport, but the two dialogs may diverge
  // further in the future.
  private void doExport(final IntermineInstance instance, final String user, final String pass,
      final List<StringList> lists, final boolean replace) {
    tmService.exportLists(instance, user, pass, lists.toArray(new StringList[0]), replace,
        new PendingAsyncCallback<Void>(parent, "Unable to export lists to " + instance.title()) {
          @Override
          public void handleSuccess(Void v) {
            Window.alert("The lists were successfully exported.");
          }
        });
  }

  public void enrich(final StringList probes) {
    InteractionDialog ui = new InterMineEnrichDialog(parent, "Enrich", preferredInstance) {
      @Override
      protected void userProceed(IntermineInstance instance, String user, String pass,
          boolean replace) {
        super.userProceed();
        if (probes.items() != null && probes.items().length > 0) {
          doEnrich(instance, probes, getParams());
          Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_ENRICHMENT, instance.title());
        } else {
          Window.alert("Please define and select a gene set first.");
        }
      }
    };
    ui.display("Enrichment", DialogPosition.Center);
  }

  // These tokens are only valid for 24 h.
  // In practice, the Toxygates server side session will time out before this, forcing a
  // client reload and thus renewal of the token.
  // TODO: keep track of expiry time/handle expiry gracefully
  private static Map<IntermineInstance, String> imTokens = new HashMap<IntermineInstance, String>();

  private void ensureTokenAndThen(final IntermineInstance instance, final Runnable r) {
    String oldToken = imTokens.get(instance);
    if (oldToken != null) {
      logger.info("Reusing session token for " + instance.title() + ": " + oldToken);
      r.run();
      return;
    }

    tmService.getSession(instance, new PendingAsyncCallback<String>(parent,
        "Failed to obtain Intermine session token") {
      @Override
      public void handleSuccess(String token) {
        logger.info("Got session token for " + instance.title() + ": " + token);
        imTokens.put(instance, token);
        r.run();
      }
    });

  }

  public void doEnrich(final IntermineInstance instance, final StringList list, 
                       final EnrichmentParams params) {
    ensureTokenAndThen(instance, new Runnable() {
      @Override
      public void run() {
        tmService.enrichment(instance, list, params, imTokens.get(instance),
            new PendingAsyncCallback<String[][]>(parent, "Unable to perform enrichment analysis") {
              @Override
              public void handleSuccess(String[][] result) {
                StringArrayTable.displayDialog(result, "Enrichment results", 800, 600);
              }
            });
      }
    });
  }

  public void multiEnrich(final StringList[] lists) {
    InteractionDialog ui =
        new InterMineEnrichDialog(parent, "Cluster enrichment", preferredInstance) {
          @Override
          protected void userProceed(IntermineInstance instance, String user, String pass,
              boolean replace) {
            super.userProceed();
            doEnrich(instance, lists, getParams());
            Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ENRICH_CLUSTERS,
                instance.title());
          }
        };
    ui.display("Cluster enrichment", DialogPosition.Center);
  }

  private static String[] append(String[] first, String[] last) {
    String[] r = new String[first.length + last.length];
    for (int i = 0; i < first.length; ++i) {
      r[i] = first[i];
    }
    for (int i = 0; i < last.length; ++i) {
      r[i + first.length] = last[i];
    }
    return r;
  }

  public void doEnrich(final IntermineInstance instance, final StringList[] lists, 
                       final EnrichmentParams params) {
    ensureTokenAndThen(instance, new Runnable() {
      @Override
      public void run() {

        tmService.multiEnrichment(
            instance,
            lists,
            params,
            imTokens.get(instance),
            new PendingAsyncCallback<String[][][]>(parent, "Unable to perform enrichment analysis") {
              @Override
              public void handleSuccess(String[][][] result) {
                List<String[]> best = new ArrayList<String[]>();
                best.add(append(new String[] {"Cluster", "Size"}, result[0][0])); // Headers
                int i = 1;
                for (String[][] clust : result) {
                  int n = lists[i - 1].size();
                  if (clust.length < 2) {
                    // TODO don't hardcode length here
                    String[] res = new String[] {"Cluster " + i, "" + n, "(No result)", "", "", ""};
                    best.add(res);
                  } else {
                    best.add(append(new String[] {"Cluster " + i, "" + n}, clust[1]));
                  }
                  i += 1;
                }
                StringArrayTable.displayDialog(best.toArray(new String[0][0]),
                    "Best enrichment results", 800, 400);
              }
            });
      }
    });
  }

  List<ItemList> mergeLists(List<? extends ItemList> into, List<? extends ItemList> from,
      boolean replace, String kind) {
    Map<String, ItemList> allLists = new HashMap<String, ItemList>();
    int addedLists = 0;
    int addedItems = 0;
    int nonImported = 0;

    for (ItemList l : into) {
      allLists.put(l.name(), l);
    }

    for (ItemList l : from) {
      if (replace || !allLists.containsKey(l.name())) {
        allLists.put(l.name(), l);
        addedLists++;
        addedItems += l.size();

        logger.info("Import list " + l.name() + " size " + l.size());
      } else if (allLists.containsKey(l.name())) {
        logger.info("Do not import list " + l.name());
        nonImported += 1;
      }
    }

    String msg = addedLists + " " + kind + " with " + addedItems + 
        " items were successfully imported.";


    if (nonImported > 0) {
      msg = msg + "\n" + nonImported + " " + kind + 
          " with identical names were not imported.";
    }

    Window.alert(msg);
    return new ArrayList<ItemList>(allLists.values());
  }
}
