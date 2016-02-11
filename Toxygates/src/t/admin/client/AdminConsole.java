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

import t.common.client.Resources;
import t.common.client.maintenance.ListDataCallback;
import t.common.client.maintenance.ManagerPanel;
import t.common.client.maintenance.BatchPanel;
import t.common.client.maintenance.ManagedItemEditor;
import t.common.client.maintenance.TaskCallback;
import t.common.shared.Dataset;
import t.common.shared.Platform;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

/**
 * Entry point for the data and instance management tool.
 */
public class AdminConsole implements EntryPoint {

  private RootLayoutPanel rootPanel;
  protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
      .create(MaintenanceService.class);

  private final Resources resources = GWT.create(Resources.class);
  
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
    ManagerPanel<Instance> ip = new ManagerPanel<Instance>("Edit instance", resources,
        true, false) {
      protected ManagedItemEditor makeEditor(Instance i, final DialogBox db, boolean addNew) {
        return new InstanceEditor(i, addNew) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshInstances();
          }
        };
      }

      protected void onDelete(Instance i) {
        deleteInstance(i);
      }
    };
    instanceData.addDataDisplay(ip.table());

    refreshInstances();
    refreshDatasets();
    return ip.panel();
  }

  private Widget makeDatasetPanel() {
    ManagerPanel<Dataset> dp = new ManagerPanel<Dataset>("Edit datasets", resources,
        true, false) {
      protected ManagedItemEditor makeEditor(Dataset d, final DialogBox db, boolean addNew) {
        return new DatasetEditor(d, addNew) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshDatasets();
          }
        };
      }

      protected void onDelete(Dataset d) {
        deleteDataset(d);
      }

      protected void addMidColumns(CellTable<Dataset> table) {
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
    datasetData.addDataDisplay(dp.table());

    refreshDatasets();
    return dp.panel();
  }

  private Widget makePlatformPanel() {
    ManagerPanel<Platform> pp = new ManagerPanel<Platform>("Edit platform", resources,
        true, false) {
      protected Widget makeEditor(Platform p, final DialogBox db, boolean addNew) {
        return new PlatformEditor(p, addNew) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            refreshPlatforms();
          }
        };
      }

      protected void onDelete(Platform p) {
        deletePlatform(p);
      }

      protected void addMidColumns(CellTable<Platform> table) {
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


    platformData.addDataDisplay(pp.table());
    refreshPlatforms();
    return pp.panel();
  }

  private Widget makeBatchPanel() {    
    BatchPanel bp = new BatchPanel("Edit batch", maintenanceService, resources,
        true, false) {
      @Override
      protected Widget makeEditor(Batch b, final DialogBox db, boolean addNew) {
        return new FullBatchEditor(b, addNew, datasetData.getList(), instanceData.getList()) {
          @Override
          protected void onFinishOrAbort() {
            db.hide();
            doRefresh();
          }
        };
      }
      
      @Override
      protected void onDelete(Batch b) {
        deleteBatch(b);
      }
      
      @Override
      protected void doRefresh() {
        refreshBatches();
      }
    };
    batchData.addDataDisplay(bp.table());
    refreshBatches();
    return bp.panel();
  }

  private void deleteBatch(final Batch object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the batch " + title + "?")) {
      return;
    }
    maintenanceService.deleteBatchAsync(object.getTitle(), 
        new TaskCallback("Delete batch", maintenanceService) {
      @Override
      protected void onCompletion() {
        refreshBatches();
      }
    });
  }

  private void deletePlatform(final Platform object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the platform " + title + "?")) {
      return;
    }
    maintenanceService.deletePlatformAsync(object.getTitle(), 
        new TaskCallback("Delete platform", maintenanceService) {
      @Override
      protected void onCompletion() {
        refreshPlatforms();
      }
    });
  }

  private void deleteInstance(final Instance object) {
    String title = object.getTitle();
    if (!Window.confirm("Are you sure you want to delete the instance " + title + "?")) {
      return;
    }
    maintenanceService.delete(object, new AsyncCallback<Void>() {

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
    maintenanceService.delete(object, new AsyncCallback<Void>() {

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

  private void refreshBatches() {
    maintenanceService.getBatches(null, new ListDataCallback<Batch>(batchData, "batch list"));
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
