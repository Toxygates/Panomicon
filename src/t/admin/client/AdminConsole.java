package t.admin.client;

import static t.admin.client.Utils.makeButtons;

import java.util.ArrayList;
import java.util.List;

import t.admin.shared.Batch;
import t.admin.shared.Dataset;
import t.admin.shared.Instance;
import t.admin.shared.ManagedItem;
import t.admin.shared.Platform;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
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
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		
		CellTable<Instance> table = makeTable();
		instanceData.addDataDisplay(table);
		
		List<Command> cmds = new ArrayList<Command>();
		cmds.add(new Command("Add new...") {
			public void run() {
				final DialogBox db = new DialogBox(false, true);
				db.setTitle("Add instance");
				db.setWidget(new InstanceEditor() {

					@Override
					protected void onFinish() {
						db.hide();
						refreshInstances();			
					}

					@Override
					protected void onAbort() {
						db.hide();
						refreshInstances();			
					}					
				});
				db.show();			
			}
		});
		
		StandardColumns<Instance> sc = new StandardColumns<Instance>(table) {
			void onDelete(Instance object) {
				deleteInstance(object);
			}
		};
		
		sc.addStartColumns();
		sc.addDeleteColumn();
	
		dp.addSouth(makeButtons(cmds), 35);		
		dp.add(table);
		
		refreshInstances();
		return dp; 
	}
	
	//TODO reduce duplicated code
	private Widget makeDatasetPanel() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);

		CellTable<Dataset> table = makeTable();
		datasetData.addDataDisplay(table);

		List<Command> cmds = new ArrayList<Command>();
		cmds.add(new Command("Add new...") {
			public void run() {
				final DialogBox db = new DialogBox(false, true);
				db.setTitle("Add instance");
				db.setWidget(new DatasetEditor() {

					@Override
					protected void onFinish() {
						db.hide();
						refreshDatasets();
					}

					@Override
					protected void onAbort() {
						db.hide();
						refreshDatasets();
					}
				});
				db.show();
			}
		});

		StandardColumns<Dataset> sc = new StandardColumns<Dataset>(table) {
			void onDelete(Dataset object) {
				deleteDataset(object);
			}
		};

		sc.addStartColumns();
		
		TextColumn<Dataset> textColumn = new TextColumn<Dataset>() {
			@Override
			public String getValue(Dataset object) {
				return "" + object.getDescription();
			}
		};
		
		table.addColumn(textColumn, "Description");
		table.setColumnWidth(textColumn, "12.5em");
		
		sc.addDeleteColumn();

		dp.addSouth(makeButtons(cmds), 35);
		dp.add(table);

		refreshDatasets();
		return dp;
	}
	
	private Widget makePlatformPanel() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		
		CellTable<Platform> table = makeTable();
		platformData.addDataDisplay(table);		
		
		StandardColumns<Platform> sc = new StandardColumns<Platform>(table) {
			void onDelete(Platform object) {
				deletePlatform(object);
			}
		};
		
		sc.addStartColumns();
		
		TextColumn<Platform> textColumn = new TextColumn<Platform>() {
			@Override
			public String getValue(Platform object) {
				return "" + object.getNumProbes();
			}
		};
		
		table.addColumn(textColumn, "Probes");
		table.setColumnWidth(textColumn, "12.5em");
		
		sc.addDeleteColumn();
		
		List<Command> cmds = new ArrayList<Command>();
		cmds.add(new Command("Upload new...") {
			public void run() {
				final DialogBox db = new DialogBox(false, true);
				db.setWidget(new PlatformUploader() {
					public void onOK() {
						db.hide();
						refreshPlatforms();
					}
					
					public void onCancel() {
						db.hide();
					}
				});
				db.setText("Upload platform");
				db.setWidth("500px");
				db.show();
			} 
		});
		
		
		dp.addSouth(makeButtons(cmds), 35);
		dp.add(table);
		refreshPlatforms();
		return dp;
	}
	
	private Widget makeBatchPanel() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		
		final CellTable<Batch> table = makeTable();
		StandardColumns<Batch> sc = new StandardColumns<Batch>(table) {
			void onDelete(Batch object) {
				deleteBatch(object);
			}
		};
		sc.addStartColumns();
		
		TextColumn<Batch> samplesColumn = new TextColumn<Batch>() {
			@Override
			public String getValue(Batch object) {
				return "" + object.getNumSamples();
			}
		};

		table.addColumn(samplesColumn, "Samples");
		table.setColumnWidth(samplesColumn, "12.5em");

		List<String> datasets = new ArrayList<String>();
		//TODO handle updates smoothly
		for (Dataset d: datasetData.getList()) {
			datasets.add(d.getTitle());
		}
		
		SelectionCell datasetCell = new SelectionCell(datasets);
		Column<Batch, String> datasetColumn = new Column<Batch, String>(datasetCell) {
			public String getValue(Batch b) {
				return "dataset";
			}
		};
		datasetColumn.setFieldUpdater(new FieldUpdater<Batch, String>() {
			public void update(int index, final Batch object, String value) {
				editDataset(object, value);				
			}
		});
		table.addColumn(datasetColumn, "Dataset");
		
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
		batchData.addDataDisplay(table);		
		
		ButtonCell editCell = new ButtonCell();
		Column<Batch, String> editColumn = new Column<Batch, String>(editCell) {
			public String getValue(Batch b) {
				return "Edit...";
			}
		};
		editColumn.setFieldUpdater(new FieldUpdater<Batch, String>() {
			@Override
			public void update(int index, final Batch object, String value) {
				editVisibility(table, object);				
			}
			
		});		
		table.addColumn(editColumn);
		
	
	
		sc.addDeleteColumn();
		
		List<Command> commands = new ArrayList<Command>();
		commands.add(new Command("Upload new...") {
			public void run() {
				final DialogBox db = new DialogBox(false, true);
				db.setWidget(new BatchUploader() {
					public void onOK() {
						db.hide();
						refreshBatches();
					}
					
					public void onCancel() {
						db.hide();						
					}					
				});
				db.setText("Upload batch");
				db.setWidth("500px");
				db.show();
			}
		});	
		
		dp.addSouth(makeButtons(commands), 35);
		dp.add(table);
		refreshBatches();
		return dp;
	}
	
	private void editVisibility(final CellTable<Batch> table, final Batch object) {
		final DialogBox db = new DialogBox(true, true);				
		db.setWidget(new VisibilityEditor(object, instanceData.getList()) {
			public void onOK() {
				object.setEnabledInstances(getSelection());
				maintenanceService.updateBatch(object,new AsyncCallback<Void>() {
					@Override
					public void onFailure(Throwable caught) {
						Window.alert("Unable to edit visibility: " + caught.getMessage());
					}

					@Override
					public void onSuccess(Void result) {
						refreshBatches();						
					}
					
				});
				table.redraw();
				db.hide();
			}
			
			public void onCancel() {
				db.hide();
			}
		});
		db.setText("Change visibility of '" + object.getTitle() + "'");
//		db.setSize("300px", "300px");
		db.setWidth("500px");
		db.show();	
	}
	
	private void editDataset(final Batch batch, final String dataset) {
		//TODO
	}
	
	private void deleteBatch(final Batch object) {
		String title = object.getTitle();
		if (!Window.confirm("Are you sure you want to delete the batch " + title + "?")) {
			return;
		}
		maintenanceService.deleteBatchAsync(object.getTitle(),
				new TaskCallback("Delete batch") {
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
		maintenanceService.deletePlatformAsync(object.getTitle(),
				new TaskCallback("Delete platform") {
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
	
	//TODO reduce duplicated code
	private void deleteDataset(final Dataset object) {
		String title = object.getTitle();
		if (!Window.confirm("Are you sure you want to delete the dataset " + title + "? Batches will not be deleted.")) {
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


	private <T extends ManagedItem> CellTable<T> makeTable() {
		CellTable<T> table = new CellTable<T>();		
//		TextColumn<T> textColumn = new TextColumn<T>() {
//			@Override
//			public String getValue(T object) {
//				return object.getTitle();
//			}
//		};				
//		table.addColumn(textColumn, "ID");
//		table.setColumnWidth(textColumn, "12.5em");
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
	
	//TODO reduce duplicated code
	private void refreshDatasets() {
		maintenanceService.getDatasets(new ListDataCallback<Dataset>(datasetData, "platform list"));
	}

}
