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

package t.admin.client;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.admin.shared.Platform;
import t.common.shared.Dataset;
import t.common.shared.ManagedItem;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * Entry point for the data and instance management tool.
 */
public class AdminConsole implements EntryPoint {

  private RootLayoutPanel rootPanel;
  protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
      .create(MaintenanceService.class);

  // TODO lift these into AdminPanel, reduce code duplication
  final ListDataProvider<Batch> batchData = new ListDataProvider<Batch>();
  final ListDataProvider<Platform> platformData = new ListDataProvider<Platform>();
  final ListDataProvider<Instance> instanceData = new ListDataProvider<Instance>();
  final ListDataProvider<Dataset> datasetData = new ListDataProvider<Dataset>();

  @Override
  public void onModuleLoad() {
    rootPanel = RootLayoutPanel.get();
    rootPanel.add(makeTabPanel());
  }

  private Widget makeTabPanel() {
    TabLayoutPanel tlp = new TabLayoutPanel(2, Unit.EM);
    tlp.add(makePlatformPanel(), "Platforms");
    tlp.add(makeBatchPanel(), "Batches");
    tlp.add(makeDatasetPanel(), "Datasets");
    tlp.add(makeInstancePanel(), "Instances");
    return tlp;
  }

  private Widget makeInstancePanel() {
    AdminPanel<Instance> ip = new AdminPanel<Instance>("Edit instance", null) {
      ManagedItemEditor makeEditor(Instance i, final DialogBox db, boolean addNew) {
        return new InstanceEditor(addNew) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshInstances();
          }
        };
      }

      void onDelete(Instance i) {
        deleteInstance(i);
      }
    };
    instanceData.addDataDisplay(ip.table);

    refreshInstances();
    refreshDatasets();
    return ip.panel();
  }

  private Widget makeDatasetPanel() {
    AdminPanel<Dataset> dp = new AdminPanel<Dataset>("Edit datasets", null) {
      ManagedItemEditor makeEditor(Dataset d, final DialogBox db, boolean addNew) {
        return new InstanceEditor(addNew) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshDatasets();
          }
        };
      }

      void onDelete(Dataset d) {
        deleteDataset(d);
      }

      void addMidColumns(CellTable<Dataset> table) {
        TextColumn<Dataset> textColumn = new TextColumn<Dataset>() {
          @Override
          public String getValue(Dataset object) {
            return "" + object.getDescription();
          }
        };

        table.addColumn(textColumn, "Description");
        table.setColumnWidth(textColumn, "12.5em");
      }
    };
    datasetData.addDataDisplay(dp.table);

    refreshDatasets();
    return dp.panel();
  }

  private Widget makePlatformPanel() {
    AdminPanel<Platform> pp = new AdminPanel<Platform>("Edit platform", null) {
      Widget makeEditor(Platform p, final DialogBox db, boolean addNew) {
        return new PlatformEditor(addNew) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshPlatforms();
          }
        };
      }

      void onDelete(Platform p) {
        deletePlatform(p);
      }

      void addMidColumns(CellTable<Platform> table) {
        TextColumn<Platform> textColumn = new TextColumn<Platform>() {
          @Override
          public String getValue(Platform object) {
            return "" + object.getNumProbes();
          }
        };

        table.addColumn(textColumn, "Probes");
        table.setColumnWidth(textColumn, "12.5em");
      }
    };


    platformData.addDataDisplay(pp.table);
    refreshPlatforms();
    return pp.panel();
  }

  private Widget makeBatchPanel() {
    AdminPanel<Batch> bp = new AdminPanel<Batch>("Edit batch", null) {
      Widget makeEditor(Batch b, final DialogBox db, boolean addNew) {
        return new BatchEditor(b, addNew, datasetData.getList(), instanceData.getList()) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshBatches();
          }
        };
      }

      void onDelete(Batch b) {
        deleteBatch(b);
      }

      void addMidColumns(CellTable<Batch> table) {
        TextColumn<Batch> samplesColumn = new TextColumn<Batch>() {
          @Override
          public String getValue(Batch object) {
            return "" + object.getNumSamples();
          }
        };

        table.addColumn(samplesColumn, "Samples");
        table.setColumnWidth(samplesColumn, "12.5em");

        TextColumn<Batch> dsColumn = new TextColumn<Batch>() {
          @Override
          public String getValue(Batch object) {
            return "" + object.getDataset();
          }
        };
        table.addColumn(dsColumn, "Dataset");
        table.setColumnWidth(dsColumn, "12.5em");

        TextColumn<Batch> visibilityColumn = new TextColumn<Batch>() {
          @Override
          public String getValue(Batch object) {
            StringBuilder sb = new StringBuilder();
            for (String inst : object.getEnabledInstances()) {
              sb.append(inst);
              sb.append(", ");
            }
            String r = sb.toString();
            if (r.length() > 2) {
              return r.substring(0, r.length() - 2);
            } else {
              return "";
            }
          }
        };

        table.addColumn(visibilityColumn, "Visibility");
      }
    };

    batchData.addDataDisplay(bp.table);
    refreshBatches();
    return bp.panel();
  }

  private void deleteBatch(final Batch object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the batch " + title + "?")) {
      return;
    }
    maintenanceService.deleteBatchAsync(object.getTitle(), new TaskCallback("Delete batch") {
      @Override
      void onCompletion() {
        refreshBatches();
      }
    });
  }

  private void deletePlatform(final Platform object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the platform " + title + "?")) {
      return;
    }
    maintenanceService.deletePlatformAsync(object.getTitle(), new TaskCallback("Delete platform") {
      @Override
      void onCompletion() {
        refreshPlatforms();
      }
    });
  }

  private void deleteInstance(final Instance object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the instance " + title + "?")) {
      return;
    }
    maintenanceService.deleteInstance(object.getTitle(), new AsyncCallback<Void>() {

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Unable to delete instance: " + caught.getMessage());
      }

      @Override
      public void onSuccess(Void result) {
        refreshInstances();
      }
    });
  }

  // TODO reduce duplicated code
  private void deleteDataset(final Dataset object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the dataset " + title
        + "? Batches will not be deleted.")) {
      return;
    }
    maintenanceService.deleteDataset(object.getTitle(), new AsyncCallback<Void>() {

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Unable to delete dataset: " + caught.getMessage());
      }

      @Override
      public void onSuccess(Void result) {
        refreshDatasets();
      }
    });
  }


  static <T extends ManagedItem> CellTable<T> makeTable() {
    CellTable<T> table = new CellTable<T>();
    table.setSelectionModel(new NoSelectionModel<T>());
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    return table;
  }

  private void refreshBatches() {
    maintenanceService.getBatches(new ListDataCallback<Batch>(batchData, "batch list"));
  }

  private void refreshInstances() {
    maintenanceService.getInstances(new ListDataCallback<Instance>(instanceData, "instance list"));
  }

  private void refreshPlatforms() {
    maintenanceService.getPlatforms(new ListDataCallback<Platform>(platformData, "platform list"));
  }

  // TODO reduce duplicated code
  private void refreshDatasets() {
    maintenanceService.getDatasets(new ListDataCallback<Dataset>(datasetData, "platform list"));
  }

}
