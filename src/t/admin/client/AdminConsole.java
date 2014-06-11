package t.admin.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import otgviewer.shared.Group;
import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.admin.shared.Platform;
import t.admin.shared.TitleItem;
import bioweb.client.components.SelectionTable;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

import static t.admin.client.Utils.*;

/**
 * Entry point for the data and instance management tool.
 */
public class AdminConsole implements EntryPoint {

	private RootLayoutPanel rootPanel;
	
	final Instance[] instances = new Instance[] {
			new Instance("Toxygates"),
			new Instance("Adjuvant"),
			new Instance("Private")
	};
	
	final Batch[] batches = new Batch[] {
			new Batch("Open TG-Gates", 18000, 
					new String[] {"Toxygates", "Adjuvant", "Private"}), 
			new Batch("Rat adjuvant", 200,
					new String[] {"Adjuvant", "Private"}), 
			new Batch("Mouse adjuvant", 200,
					new String[] {"Adjuvant", "Private"}), 
			new Batch("Secret", 300,
					new String[] { "Private" })
			};
	
	final Platform[] platforms = new Platform[] {
			new Platform("Affymetrix Human", 50000),
			new Platform("Affymetrix Rat", 30000),
			new Platform("Affymetrix Mouse", 40000)
			};
	
	@Override
	public void onModuleLoad() {
		rootPanel = RootLayoutPanel.get();	
		rootPanel.add(makeTabPanel());
	}
	
	private Widget makeInstanceEditor() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		
		ListDataProvider<Instance> p = new ListDataProvider<Instance>(Arrays.asList(instances));
		
		CellTable<Instance> table = makeTable();
		p.addDataDisplay(table);
		
		List<Command> cmds = new ArrayList<Command>();
		cmds.add(new Command("Add new...") {
			public void run() {}
		});
		cmds.add(new Command("Delete") {
			public void run() {}
		});		
		dp.addSouth(makeButtons(cmds), 35);		
		dp.add(table);
		
		return dp; 
	}

	
	private Widget makeTabPanel() {
		TabLayoutPanel tlp = new TabLayoutPanel(2, Unit.EM);
		tlp.add(makeInstanceEditor(), "Instances");
		tlp.add(makeBatchEditor(), "Batches");
		tlp.add(makePlatformEditor(), "Platforms");
		tlp.add(makeAccessEditor(), "Access");				
		return tlp;
	}
	
	private Widget makePlatformEditor() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		ListDataProvider<Platform> p = new ListDataProvider<Platform>(Arrays.asList(platforms));
		
		CellTable<Platform> table = makeTable();
		p.addDataDisplay(table);
		
		TextColumn<Platform> textColumn = new TextColumn<Platform>() {
			@Override
			public String getValue(Platform object) {
				return "" + object.getNumProbes();
			}
		};
		
		table.addColumn(textColumn, "Probes");
		table.setColumnWidth(textColumn, "12.5em");
		
		List<Command> cmds = new ArrayList<Command>();
		cmds.add(new Command("Upload new...") {
			public void run() {} 
		});
		cmds.add(new Command("Delete") {
			public void run() {}
		});
		
		dp.addSouth(makeButtons(cmds), 35);
		dp.add(table);
		return dp;
	}
	
	private Widget makeBatchEditor() {
		DockLayoutPanel dp = new DockLayoutPanel(Unit.PX);
		
		final CellTable<Batch> table = makeTable();
				
		TextColumn<Batch> samplesColumn = new TextColumn<Batch>() {
			@Override
			public String getValue(Batch object) {
				return "" + object.getNumSamples();
			}
		};

		table.addColumn(samplesColumn, "Samples");
		table.setColumnWidth(samplesColumn, "12.5em");

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
		ListDataProvider<Batch> p = new ListDataProvider<Batch>(Arrays.asList(batches));
		p.addDataDisplay(table);		
		
		ButtonCell editCell = new ButtonCell();
		Column<Batch, String> editColumn = new Column<Batch, String>(editCell) {
			public String getValue(Batch b) {
				return "Edit...";
			}
		};
		editColumn.setFieldUpdater(new FieldUpdater<Batch, String>() {
			@Override
			public void update(int index, final Batch object, String value) {
				final DialogBox db = new DialogBox(true, true);				
				db.setWidget(new VisibilityEditor(object, Arrays.asList(instances)) {
					public void onOK() {
						object.setEnabledInstances(getSelection());
						table.redraw();
						db.hide();
					}
					
					public void onCancel() {
						db.hide();
					}
				});
				db.setText("Change visibility of '" + object.getTitle() + "'");
//				db.setSize("300px", "300px");
				db.setWidth("500px");
				db.show();
			}
			
		});
		table.addColumn(editColumn);
//		table.setSelectionModel(new NoSelectionModel<Batch>());
//		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
//		
		List<Command> commands = new ArrayList<Command>();
		commands.add(new Command("Upload new...") {
			public void run() {
				final DialogBox db = new DialogBox(true, true);
				db.setWidget(new BatchUploader() {
					public void onOK() {
						
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
		commands.add(new Command("Delete") {
			public void run() {}
		});
		commands.add(new Command("Publish changes") {
			public void run() {}
		});
		
		dp.addSouth(makeButtons(commands), 35);
		dp.add(table);
		return dp;
	}
	
	private Widget makeAccessEditor() {
		return new SimplePanel();
	}
	

	
	private <T extends TitleItem> CellTable<T> makeTable() {
		CellTable<T> table = new CellTable<T>();		
		TextColumn<T> textColumn = new TextColumn<T>() {
			@Override
			public String getValue(T object) {
				return object.getTitle();
			}
		};				
		table.addColumn(textColumn, "ID");
		table.setColumnWidth(textColumn, "12.5em");
		return table;
	}

}
