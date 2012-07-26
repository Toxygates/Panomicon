package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;
import otgviewer.shared.ValueType;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OTGViewer implements EntryPoint {

	//COMMON
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);

	enum DataSet {
		HumanVitro, RatVitro, RatVivoKidneySingle, RatVivoKidneyRepeat, RatVivoLiverSingle, RatVivoLiverRepeat
	}

	private DataSet chosenDataSet = DataSet.HumanVitro;
	private Organ chosenOrgan = Organ.Liver;
	private RepeatType chosenRepeatType = RepeatType.Single;
	private Organism chosenOrganism = Organism.Rat;
	private CellType chosenCellType = CellType.Vivo;
	private DataFilter chosenDataFilter = new DataFilter(chosenCellType, chosenOrgan, 
			chosenRepeatType, chosenOrganism);
	
	//DATA VIEWER PANEL
	
	private DataTable chartTable;
	private LineChart exprChart;
	
	private ListBox pathwayList, compoundList, doseLevelList, timeList,
			barcodeList;
	private HorizontalPanel horizontalPanel;
	
	private ListDataProvider<ExpressionRow> listDataProvider;
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	private DataGrid<ExpressionRow> exprGrid;

	private ValueType chosenValueType = ValueType.Folds;
	private ListSelectionHandler<String> compoundHandler, doseHandler,
			timeHandler, pathwayHandler;
	private MultiSelectionHandler<Barcode> barcodeHandler;

	private TextBox pathwayBox;

	// Visible columns
	private boolean geneIdColVis = false, probeColVis = false,
			probeTitleColVis = true;

	// Track the current selection
	private String[] displayedProbes = null; 
	private String chosenProbe; //single user-selected probe
	private String chosenCompound;

	//CHART PANEL
	private SeriesDisplayStrategy seriesStrategy;
	private Label seriesSelectionLabel;
	private ListBox chartCombo, chartSubtypeCombo;	
	private DataTable seriesTable;	
	private CoreChart seriesChart;
	private DockPanel chartDockPanel;
	private AsyncCallback<String[]> seriesChartItemsCallback = new AsyncCallback<String[]>() {
		public void onFailure(Throwable caught) {
			Window.alert("Unable to get series chart subitems.");
		}
		
		public void onSuccess(String[] result) {
			for (String i: result) {
				chartSubtypeCombo.addItem(i);
			}
			if (result.length > 0) {
				chartSubtypeCombo.setSelectedIndex(0);
				redrawSeriesChart();
			}
		}
	};

	private AsyncCallback<Barcode[]> seriesChartBarcodesCallback = new AsyncCallback<Barcode[]>() {
		public void onFailure(Throwable caught) {
			Window.alert("Unable to get series chart data (barcodes).");
		}
		
		public void onSuccess(Barcode[] barcodes) {
			seriesStrategy.setupTable(barcodes);
			
			List<String> bcs = new ArrayList<String>();
			for (Barcode b: barcodes) {											
				bcs.add(b.getCode());
			}
			if (chosenProbe == null) {
				Window.alert("Unable to draw chart. Please select a probe first.");
			} else {
				String[] probes = new String[] { chosenProbe };

				kcService.getFullData(bcs, probes, chosenValueType, true,
						new AsyncCallback<List<ExpressionRow>>() {
							public void onSuccess(List<ExpressionRow> result) {
								if (seriesChart != null) {
									chartDockPanel.remove(seriesChart);
								}
								seriesChart = seriesStrategy.makeChart();
								chartDockPanel.add(seriesChart,
										DockPanel.CENTER);
								seriesStrategy.displayData(result, seriesChart);
							}

							public void onFailure(Throwable caught) {
								Window.alert("Unable to get series chart data (expressions).");
	}
			});
			}
		}
	};
	
	private MenuBar setupMenu() {
	
		MenuBar menuBar = new MenuBar(false);
		menuBar.setWidth("100%");
		MenuBar menuBar_1 = new MenuBar(true);

		MenuItem mntmNewMenu = new MenuItem("New menu", false, menuBar_1);

		MenuItem mntmNewItem = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenOrganism = Organism.Human;
				chosenCellType = CellType.Vitro;
				// Repeat/single ??
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem.setHTML("Human, in vitro");
		menuBar_1.addItem(mntmNewItem);

		MenuItem mntmNewItem_1 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenOrganism = Organism.Rat;
				chosenCellType = CellType.Vitro;
				// Repeat/single ??
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_1.setHTML("Rat, in vitro");
		menuBar_1.addItem(mntmNewItem_1);

		MenuItem mntmNewItem_2 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenOrganism = Organism.Rat;
				chosenOrgan = Organ.Liver;
				chosenRepeatType = RepeatType.Single;
				chosenCellType = CellType.Vivo;
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_2.setHTML("Rat, in vivo, liver, single");
		menuBar_1.addItem(mntmNewItem_2);

		MenuItem mntmNewItem_3 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenOrganism = Organism.Rat;
				chosenOrgan = Organ.Liver;
				chosenRepeatType = RepeatType.Repeat;
				chosenCellType = CellType.Vivo;
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_3.setHTML("Rat, in vivo, liver, repeat");
		menuBar_1.addItem(mntmNewItem_3);

		MenuItem mntmNewItem_4 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenOrganism = Organism.Rat;
				chosenOrgan = Organ.Kidney;
				chosenRepeatType = RepeatType.Single;
				chosenCellType = CellType.Vivo;
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_4.setHTML("Rat, in vivo, kidney, single");
		menuBar_1.addItem(mntmNewItem_4);

		MenuItem mntmNewItem_5 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenOrganism = Organism.Rat;
				chosenOrgan = Organ.Kidney;
				chosenRepeatType = RepeatType.Repeat;
				chosenCellType = CellType.Vivo;
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_5.setHTML("Rat, in vivo, kidney, repeat");
		menuBar_1.addItem(mntmNewItem_5);

		MenuItemSeparator separator = new MenuItemSeparator();
		menuBar_1.addSeparator(separator);

		MenuItem mntmFolds = new MenuItem("Fold values", false, new Command() {
			public void execute() {
				chosenValueType = ValueType.Folds;
				updateSelections();
				getExpressions();
			}
		});
		menuBar_1.addItem(mntmFolds);

		MenuItem mntmAbsoluteValues = new MenuItem("Absolute values", false,
				new Command() {
					public void execute() {
						chosenValueType = ValueType.Absolute;
						updateSelections();
						getExpressions();
					}
				});

		menuBar_1.addItem(mntmAbsoluteValues);
		mntmNewMenu.setHTML("Data set");
		menuBar.addItem(mntmNewMenu);
		MenuBar menuBar_2 = new MenuBar(true);

		MenuItem mntmNewMenu_1 = new MenuItem("New menu", false, menuBar_2);

		MenuItem mntmGeneId = new MenuItem("Gene ID", false, new Command() {
			public void execute() {
				geneIdColVis = !geneIdColVis;
				setupColumns();
			}
		});
		menuBar_2.addItem(mntmGeneId);

		MenuItem mntmProbeName = new MenuItem("Probe name", false,
				new Command() {
					public void execute() {
						probeColVis = !probeColVis;
						setupColumns();
					}
				});
		menuBar_2.addItem(mntmProbeName);

		MenuItem mntmGeneName = new MenuItem("Gene name", false, new Command() {
			public void execute() {
				probeTitleColVis = !probeTitleColVis;
				setupColumns();
			}
		});

		menuBar_2.addItem(mntmGeneName);
		mntmNewMenu_1.setHTML("Columns");
		menuBar.addItem(mntmNewMenu_1);

		MenuItem mntmSettings = new MenuItem("Settings", false, (Command) null);
		menuBar.addItem(mntmSettings);
		return menuBar;
	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		Runnable onLoadChart = new Runnable() {
			public void run() {
				Options lco = Options.create();
				chartTable = DataTable.create();
				exprChart = new LineChart(chartTable, lco);
				exprChart.setWidth("300px");
				exprChart.setHeight("200px");
				horizontalPanel.add(exprChart);
				
				seriesTable = DataTable.create();				
			}
		};

		VisualizationUtils.loadVisualizationApi(onLoadChart, "corechart");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.setSize("100%", "100%");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);

		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3.setBorderWidth(0);
		rootPanel.add(verticalPanel_3);
		verticalPanel_3.setSize("100%", "100%");

		verticalPanel_3.add(setupMenu());
		

		HorizontalSplitPanel horizontalSplitPanel = new HorizontalSplitPanel();
		horizontalSplitPanel.setSplitPosition("200px");
		verticalPanel_3.add(horizontalSplitPanel);
		horizontalSplitPanel.setSize("100%", "800px");

		//PATHWAY SEARCH
		VerticalPanel verticalPanel_2 = new VerticalPanel();
		verticalPanel_2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		horizontalSplitPanel.setLeftWidget(verticalPanel_2);
		verticalPanel_2.setBorderWidth(1);
		verticalPanel_2.setSize("100%", "");

		Label lblPathwaySearch = new Label("Pathway search");
		verticalPanel_2.add(lblPathwaySearch);
		lblPathwaySearch.setWidth("100%");

		pathwayBox = new TextBox();
		verticalPanel_2.add(pathwayBox);
		pathwayBox.setWidth("90%");
		pathwayBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					getPathways(pathwayBox.getText());
				}
			}
		});

		pathwayList = new ListBox();
		verticalPanel_2.add(pathwayList);
		pathwayList.setSize("100%", "389px");
		pathwayList.setVisibleItemCount(5);

		pathwayHandler = new ListSelectionHandler<String>("pathways",
				pathwayList, false) {
			protected void getUpdates(String pathway) {
				owlimService.probesForPathway(pathway, new AsyncCallback<String[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to get probes.");
					}

					public void onSuccess(String[] probes) {
						displayedProbes = probes;
						getExpressions();
					}
				});
			}
		};

		Button btnShowAllProbes = new Button("Show all probes");
		verticalPanel_2.add(btnShowAllProbes);
		btnShowAllProbes.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				displayedProbes = null;
				if (pathwayList.getSelectedIndex() != -1) {
					pathwayList.setItemSelected(pathwayList.getSelectedIndex(),
							false);
				}
				getExpressions();
			}
		});
		
		Button btnShowCompoundTargets = new Button("Show CHEMBL targets");
		verticalPanel_2.add(btnShowCompoundTargets);
		btnShowCompoundTargets.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (chosenCompound != null) {
					owlimService.probesTargetedByCompound(chosenCompound, new AsyncCallback<String[]>() {
						public void onFailure(Throwable caught) {
							Window.alert("Unable to get probes.");
						}

						public void onSuccess(String[] probes) {
							displayedProbes = probes;
							getExpressions();
						}
					});
				} else {
					Window.alert("Please select a compound first.");
				}
			}
		});

		TabPanel tabPanel = new TabPanel();
		horizontalSplitPanel.setRightWidget(tabPanel);
		tabPanel.setSize("100%", "100%");
		tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
			public void onSelection(SelectionEvent<Integer> event) {
				switch(event.getSelectedItem()) {
				case 0:
					//data viewer tab
					break;
				case 1:
					//series chart tab
					if (chartCombo.getSelectedIndex() == -1) {
						chartCombo.setSelectedIndex(0);
						updateSeriesSubtypes();
					} else {
						redrawSeriesChart();
					}
				}
			}
		});

		//DATA VIEWER UI
		DockPanel dockPanel = new DockPanel();
		tabPanel.add(dockPanel, "Data viewer", false);
		dockPanel.setSize("100%", "100%");
		tabPanel.selectTab(0);

		horizontalPanel = new HorizontalPanel();
		dockPanel.add(horizontalPanel, DockPanel.NORTH);
		horizontalPanel.setWidth("549px");

		VerticalPanel verticalPanel_1 = new VerticalPanel();
		horizontalPanel.add(verticalPanel_1);

		Label label = new Label("Compounds");
		verticalPanel_1.add(label);

		ListBox compoundList_1 = new ListBox();
		compoundList_1.setVisibleItemCount(10);
		verticalPanel_1.add(compoundList_1);
		compoundList_1.setSize("210px", "218px");

		compoundHandler = new ListSelectionHandler<String>("compounds",
				compoundList_1, false) {
			protected void getUpdates(String compound) {
				chosenCompound = compound;
				updateSelections();
				getDoseLevels(compound, chosenOrgan.toString());
				getTimes(compound, chosenOrgan.toString());
			}
		};

		VerticalPanel verticalPanel_4 = new VerticalPanel();
		horizontalPanel.add(verticalPanel_4);

		Label label_1 = new Label("Doses");
		verticalPanel_4.add(label_1);

		doseLevelList = new ListBox();
		doseLevelList.setVisibleItemCount(10);
		verticalPanel_4.add(doseLevelList);
		doseLevelList.setSize("10em", "100px");

		doseHandler = new ListSelectionHandler<String>("dose levels",
				doseLevelList, true) {
			protected void getUpdates(String dose) {
				updateSelections();
				getBarcodes(compoundHandler.lastSelected(),
						chosenOrgan.toString(), doseHandler.lastSelected(),
						timeHandler.lastSelected());

			}
		};

		Label label_2 = new Label("Times");
		verticalPanel_4.add(label_2);

		timeList = new ListBox();
		timeList.setVisibleItemCount(5);
		verticalPanel_4.add(timeList);
		timeList.setSize("10em", "100px");

		timeHandler = new ListSelectionHandler<String>("times", timeList, true) {
			protected void getUpdates(String time) {
				updateSelections();
				getBarcodes(compoundHandler.lastSelected(),
						chosenOrgan.toString(), doseHandler.lastSelected(),
						timeHandler.lastSelected());
			}
		};

		VerticalPanel verticalPanel_5 = new VerticalPanel();
		horizontalPanel.add(verticalPanel_5);

		Label label_3 = new Label("Arrays");
		verticalPanel_5.add(label_3);

		barcodeList = new ListBox();
		barcodeList.setVisibleItemCount(10);
		barcodeList.setMultipleSelect(true);
		verticalPanel_5.add(barcodeList);
		barcodeList.setSize("15em", "218px");

		barcodeHandler = new MultiSelectionHandler<Barcode>("barcodes",
				barcodeList) {
			protected void getUpdates(String barcode) {

			}

			protected void getUpdates(List<Barcode> barcodes) {
				getExpressions();
			}

			protected String representation(Barcode b) {
				return b.getTitle();
			}
		};

		DockPanel dockPanel_1 = new DockPanel();
		dockPanel.add(dockPanel_1, DockPanel.CENTER);
		dockPanel_1.setSize("100%", "100%");

		SimplePager.Resources pagerResources = GWT
				.create(SimplePager.Resources.class);

		SimplePager exprPager = new SimplePager(TextLocation.CENTER,
				pagerResources, true, 100, true);
		dockPanel_1.add(exprPager, DockPanel.NORTH);

		exprGrid = new DataGrid<ExpressionRow>();
		dockPanel_1.add(exprGrid, DockPanel.CENTER);
		exprGrid.setStyleName("exprGrid");
		exprGrid.setPageSize(20);
		exprGrid.setSize("100%", "400px");
		exprGrid.setSelectionModel(new MultiSelectionModel<ExpressionRow>());
		exprGrid.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {
				for (ExpressionRow r: exprGrid.getDisplayedItems()) {
					if (exprGrid.getSelectionModel().isSelected(r)) {
						chosenProbe = r.getProbe();
						updateSelections();
					}
				}		
			}
		});
		asyncProvider.addDataDisplay(exprGrid);
		exprPager.setDisplay(exprGrid);

		listDataProvider = new ListDataProvider<ExpressionRow>();

		compoundHandler.addAfter(doseHandler);
		compoundHandler.addAfter(timeHandler);
		doseHandler.addAfter(barcodeHandler);
		timeHandler.addAfter(barcodeHandler);

		
		// CHART PANEL GUI
		chartDockPanel = new DockPanel();
		tabPanel.add(chartDockPanel, "Chart", false);		
		chartDockPanel.setSize("100%", "100%");
		
		VerticalPanel verticalPanel = new VerticalPanel();
		chartDockPanel.add(verticalPanel, DockPanel.NORTH);
		verticalPanel.setWidth("276px");
		
		seriesSelectionLabel = new Label("Selected: none");
		verticalPanel.add(seriesSelectionLabel);
		
		HorizontalPanel horizontalPanel_2 = new HorizontalPanel();
		horizontalPanel_2.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		verticalPanel.add(horizontalPanel_2);
		
				chartCombo = new ListBox();
				horizontalPanel_2.add(chartCombo);
				chartCombo.addItem("Expression vs time, fixed dose:");
				chartCombo.addItem("Expression vs dose, fixed time:");
				chartCombo.setSelectedIndex(0);
				
				chartSubtypeCombo = new ListBox();
				horizontalPanel_2.add(chartSubtypeCombo);
				
				chartSubtypeCombo.addChangeHandler(new ChangeHandler() {
					public void onChange(ChangeEvent event) {
						seriesTable.removeRows(0, seriesTable.getNumberOfRows());
						redrawSeriesChart();						
					}
					
				});
				chartCombo.addChangeHandler(new ChangeHandler() {
					public void onChange(ChangeEvent event) {
						updateSeriesSubtypes();						
					}
				});

		//INITIAL DATA
		getCompounds();
	}
	

	/**
	 * This method is called when selection variables have changed
	 * and this needs to be reflected.
	 */
	void updateSelections() {
		seriesSelectionLabel.setText("Selected: " + chosenOrganism + "/" +
	    chosenOrgan + "/"  + chosenCompound + "/" + chosenCellType + "/" +  chosenRepeatType + "/" + chosenValueType + "/" + chosenProbe);
		chosenDataFilter = new DataFilter(chosenCellType, chosenOrgan, chosenRepeatType, chosenOrganism);
	}

	//----------DATA VIEWER PANEL-----------
	
	class ExpressionColumn extends Column<ExpressionRow, Number> {
		int i;
		NumberCell nc;

		public ExpressionColumn(NumberCell nc, int i) {
			super(nc);
			this.i = i;
			this.nc = nc;
		}

		public Double getValue(ExpressionRow er) {
			if (!er.getValue(i).getPresent()) {
				return Double.NaN;
			} else {
				return er.getValue(i).getValue();
			}
		}
	}
	
	private void setupColumns() {
		// todo: explicitly set the width of each column
		NumberCell nc = new NumberCell();

		int count = exprGrid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			exprGrid.removeColumn(0);
		}

		int extraCols = 0;

		if (probeColVis) {
			TextColumn<ExpressionRow> probeCol = new TextColumn<ExpressionRow>() {
				public String getValue(ExpressionRow er) {
					return er.getProbe();
				}
			};
			exprGrid.addColumn(probeCol, "Probe");

			extraCols += 1;
		}

		if (probeTitleColVis) {
			TextColumn<ExpressionRow> titleCol = new TextColumn<ExpressionRow>() {
				public String getValue(ExpressionRow er) {
					return er.getTitle();
				}
			};
			exprGrid.addColumn(titleCol, "Title");
			extraCols += 1;
		}

		if (geneIdColVis) {
			TextColumn<ExpressionRow> geneIdCol = new TextColumn<ExpressionRow>() {
				public String getValue(ExpressionRow er) {
					return er.getGeneId();
				}
			};
			exprGrid.addColumn(geneIdCol, "Gene");
			extraCols += 1;
		}

		if (chartTable != null) {
			chartTable.removeColumns(0, chartTable.getNumberOfColumns());
			chartTable.addColumn(ColumnType.STRING, "Probe");

			int i = 0;
			List<Barcode> selection = barcodeHandler.lastMultiSelection();
			for (Barcode bc : selection) {
				Column<ExpressionRow, Number> valueCol = new ExpressionColumn(
						nc, i);
				exprGrid.addColumn(valueCol, bc.getShortTitle());
				chartTable.addColumn(ColumnType.NUMBER, bc.getShortTitle());
				i += 1;
			}
		}

	}

	void getCompounds() {
		owlimService.compounds(chosenDataFilter, compoundHandler.retrieveCallback());
	}

	void getDoseLevels(String compound, String organ) {
		doseLevelList.clear();
		owlimService.doseLevels(chosenDataFilter, compound, organ, 
				doseHandler.retrieveCallback());
	}
	
	void getDosesForSeriesChart() {
		chartSubtypeCombo.clear();
		owlimService.doseLevels(chosenDataFilter, chosenCompound, 
				chosenOrgan.toString(), seriesChartItemsCallback);
	}

	void getBarcodes(String compound, String organ, String doseLevel,
			String time) {
		barcodeList.clear();
		owlimService.barcodes(chosenDataFilter, compound, organ, doseLevel, time,
				barcodeHandler.retrieveCallback());
	}

	void getTimes(String compound, String organ) {
		timeList.clear();
		owlimService.times(chosenDataFilter, compound, organ, 
				timeHandler.retrieveCallback());
	}
	
	void getTimesForSeriesChart() {
		chartSubtypeCombo.clear();
		owlimService.times(chosenDataFilter, chosenCompound, 
				chosenOrgan.toString(), seriesChartItemsCallback);
	}

	void getExpressions() {
		exprGrid.setRowCount(0, false);
		setupColumns();
		List<String> codes = new ArrayList<String>();
		for (Barcode code : barcodeHandler.lastMultiSelection()) {
			codes.add(code.getCode());
		}

		kcService.loadDataset(codes, displayedProbes, chosenValueType,
				new AsyncCallback<Integer>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to load dataset.");
					}

					public void onSuccess(Integer result) {
						exprGrid.setRowCount(result);
						exprGrid.setVisibleRangeAndClearData(new Range(0, 20),
								true);

					}
				});
	}

	void getPathways(String pattern) {
		owlimService.pathways(pattern, pathwayHandler.retrieveCallback());
	}

	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;

		AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get expression values.");
			}

			public void onSuccess(List<ExpressionRow> result) {
				exprGrid.setRowData(start, result);

				if (chartTable != null) {
					chartTable.removeRows(0, chartTable.getNumberOfRows());
					for (int i = 0; i < result.size(); ++i) {
						chartTable.addRow();
						ExpressionRow row = result.get(i);
						int cols = barcodeHandler.lastMultiSelection().size();
						chartTable.setValue(i, 0, row.getProbe());
						for (int j = 0; j < cols; ++j) {
							chartTable.setValue(i, j + 1, row.getValue(j)
									.getValue());
						}
						exprChart.draw(chartTable);
					}
				}
			}
		};

		protected void onRangeChanged(HasData<ExpressionRow> display) {
			Range range = display.getVisibleRange();
			start = range.getStart();
			kcService.datasetItems(range.getStart(), range.getLength(),
					rowCallback);
		}

	}
	
	//---------- SERIES CHART PANEL ----------
	void redrawSeriesChart() {
		//first find the applicable barcodes
		if (chartCombo.getSelectedIndex() == 0) {
			//select for specific dose.
			owlimService.barcodes(chosenDataFilter, chosenCompound, chosenOrgan.toString(), 
					chartSubtypeCombo.getItemText(chartSubtypeCombo.getSelectedIndex()), null, 
					seriesChartBarcodesCallback);
		} else {
			//select for specific time.
			owlimService.barcodes(chosenDataFilter, chosenCompound, chosenOrgan.toString(), null, 
					chartSubtypeCombo.getItemText(chartSubtypeCombo.getSelectedIndex()), 
					seriesChartBarcodesCallback);
		}
	}
	
	void updateSeriesSubtypes() {
		chartSubtypeCombo.clear();
		if (chartCombo.getSelectedIndex() == 0) {					
			seriesStrategy = new SeriesDisplayStrategy.VsTime(seriesTable);
			getDosesForSeriesChart();
		} else {
			seriesStrategy = new SeriesDisplayStrategy.VsDose(seriesTable);
			getTimesForSeriesChart();	
		}
	}
	
}
