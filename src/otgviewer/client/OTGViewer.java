package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.ExpressionRow;
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
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.Range;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OTGViewer implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);

	private ListBox compoundList, doseLevelList, barcodeList,
		timeList, pathwayList;
	private DataGrid<ExpressionRow> exprGrid;
	private DataTable chartTable;
	private ListDataProvider<ExpressionRow> listDataProvider;
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();

	private ValueType chosenValueType = ValueType.Folds;
	private ListSelectionHandler<String> compoundHandler,
			doseHandler, timeHandler, pathwayHandler;
	private MultiSelectionHandler<Barcode> barcodeHandler;

	private TextBox pathwayBox;
	private String[] chosenProbes = null;
	private String chosenOrgan = "Liver";
	private HorizontalPanel horizontalPanel;
	
	LineChart exprChart;
	
	enum DataSet {
		HumanVitro, RatVitro, RatVivoKidneySingle, RatVivoKidneyRepeat, RatVivoLiverSingle, RatVivoLiverRepeat
	}

	private DataSet chosenDataSet = DataSet.HumanVitro;

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
			}
		};
		
		VisualizationUtils.loadVisualizationApi(onLoadChart, "corechart");
		
		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.setSize("100%", "");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);

		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3.setBorderWidth(0);
		rootPanel.add(verticalPanel_3);
		verticalPanel_3.setWidth("100%");

		MenuBar menuBar = new MenuBar(false);
		verticalPanel_3.add(menuBar);
		menuBar.setWidth("100%");
		MenuBar menuBar_1 = new MenuBar(true);

		MenuItem mntmNewMenu = new MenuItem("New menu", false, menuBar_1);

		MenuItem mntmNewItem = new MenuItem("New item", false, (Command) null);
		mntmNewItem.setHTML("Human, in vitro");
		menuBar_1.addItem(mntmNewItem);

		MenuItem mntmNewItem_1 = new MenuItem("New item", false, (Command) null);
		mntmNewItem_1.setHTML("Rat, in vitro");
		menuBar_1.addItem(mntmNewItem_1);

		Command liverSelect = new Command() {
			public void execute() {
				chosenOrgan = "Liver";
			}
		};
		
		Command kidneySelect = new Command() {
			public void execute() {
				chosenOrgan = "Kidney";
			}
		};
		
		MenuItem mntmNewItem_2 = new MenuItem("New item", false, liverSelect);
		mntmNewItem_2.setHTML("Rat, in vivo, liver, single");		
		menuBar_1.addItem(mntmNewItem_2);

		MenuItem mntmNewItem_3 = new MenuItem("New item", false, liverSelect);
		mntmNewItem_3.setHTML("Rat, in vivo, liver, repeat");
		menuBar_1.addItem(mntmNewItem_3);

		MenuItem mntmNewItem_4 = new MenuItem("New item", false, kidneySelect);
		mntmNewItem_4.setHTML("Rat, in vivo, kidney, single");
		menuBar_1.addItem(mntmNewItem_4);

		MenuItem mntmNewItem_5 = new MenuItem("New item", false, kidneySelect);
		mntmNewItem_5.setHTML("Rat, in vivo, kidney, repeat");
		menuBar_1.addItem(mntmNewItem_5);

		MenuItemSeparator separator = new MenuItemSeparator();
		menuBar_1.addSeparator(separator);

		MenuItem mntmFolds = new MenuItem("Fold values", false, new Command() {
			public void execute() {
				chosenValueType = ValueType.Folds;
				getCompounds();
			}
		});
		menuBar_1.addItem(mntmFolds);

		MenuItem mntmAbsoluteValues = new MenuItem("Absolute values", false,
				new Command() {
					public void execute() {
						chosenValueType = ValueType.Absolute;
						getCompounds();
					}
				});

		menuBar_1.addItem(mntmAbsoluteValues);
		mntmNewMenu.setHTML("Data set");
		menuBar.addItem(mntmNewMenu);
		MenuBar menuBar_2 = new MenuBar(true);

		MenuItem mntmNewMenu_1 = new MenuItem("New menu", false, menuBar_2);

		MenuItem mntmGeneId = new MenuItem("Gene ID", false, (Command) null);
		menuBar_2.addItem(mntmGeneId);

		MenuItem mntmProbeName = new MenuItem("Probe name", false,
				(Command) null);
		menuBar_2.addItem(mntmProbeName);

		MenuItem mntmGeneName = new MenuItem("Gene name", false, (Command) null);
		menuBar_2.addItem(mntmGeneName);
		mntmNewMenu_1.setHTML("Columns");
		menuBar.addItem(mntmNewMenu_1);

		MenuItem mntmSettings = new MenuItem("Settings", false, (Command) null);
		menuBar.addItem(mntmSettings);
		SimplePager.Resources pagerResources = GWT
				.create(SimplePager.Resources.class);

		HorizontalSplitPanel horizontalSplitPanel = new HorizontalSplitPanel();
		horizontalSplitPanel.setSplitPosition("200px");
		verticalPanel_3.add(horizontalSplitPanel);
		horizontalSplitPanel.setSize("100%", "700px");

		VerticalPanel verticalPanel_2 = new VerticalPanel();
		horizontalSplitPanel.setLeftWidget(verticalPanel_2);
		verticalPanel_2.setBorderWidth(1);
		verticalPanel_2.setSize("100%", "");

		Label lblPathwaySearch = new Label("Pathway search");
		verticalPanel_2.add(lblPathwaySearch);

		pathwayBox = new TextBox();
		verticalPanel_2.add(pathwayBox);
		pathwayBox.setWidth("165px");
		pathwayBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					getPathways(pathwayBox.getText());
				}
			}
		});

		pathwayList = new ListBox();
		verticalPanel_2.add(pathwayList);
		pathwayList.setSize("100%", "500px");
		pathwayList.setVisibleItemCount(5);

		pathwayHandler = new ListSelectionHandler<String>("pathways",
				pathwayList, false) {
			protected void getUpdates(String pathway) {
				owlimService.probes(pathway, new AsyncCallback<String[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to get probes.");
					}

					public void onSuccess(String[] probes) {
						chosenProbes = probes;
						getExpressions();
					}
				});
			}
		};
		
		Button btnShowAllProbes = new Button("Show all probes");
		verticalPanel_2.add(btnShowAllProbes);		
		btnShowAllProbes.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				chosenProbes = null;
				if (pathwayList.getSelectedIndex() != -1) {
					pathwayList.setItemSelected(pathwayList.getSelectedIndex(), false);
				}
				getExpressions();
			}
		});

		VerticalPanel verticalPanel = new VerticalPanel();
		horizontalSplitPanel.setRightWidget(verticalPanel);
		verticalPanel.setBorderWidth(0);
		verticalPanel.setSize("100%", "100%");

		horizontalPanel = new HorizontalPanel();
		verticalPanel.add(horizontalPanel);

		compoundList = new ListBox();
		horizontalPanel.add(compoundList);
		compoundList.setSize("210px", "202px");
		compoundList.setVisibleItemCount(10);

		compoundHandler = new ListSelectionHandler<String>("compounds",
				compoundList, false) {
			protected void getUpdates(String compound) {
				getDoseLevels(compound, chosenOrgan);
				getTimes(compound, chosenOrgan);
			}
		};

		VerticalPanel verticalPanel_1 = new VerticalPanel();
		horizontalPanel.add(verticalPanel_1);

		doseLevelList = new ListBox();
		verticalPanel_1.add(doseLevelList);
		doseLevelList.setSize("10em", "100px");
		doseLevelList.setVisibleItemCount(10);

		doseHandler = new ListSelectionHandler<String>("dose levels",
				doseLevelList, true) {
			protected void getUpdates(String dose) {
				getBarcodes(compoundHandler.lastSelected(),
						chosenOrgan,
						doseHandler.lastSelected(), timeHandler.lastSelected());

			}
		};

		timeList = new ListBox();
		verticalPanel_1.add(timeList);
		timeList.setSize("10em", "100px");
		timeList.setVisibleItemCount(5);

		timeHandler = new ListSelectionHandler<String>("times", timeList, true) {
			protected void getUpdates(String time) {
				getBarcodes(compoundHandler.lastSelected(),
						chosenOrgan,
						doseHandler.lastSelected(), timeHandler.lastSelected());
			}
		};

		barcodeList = new ListBox();
		horizontalPanel.add(barcodeList);
		barcodeList.setMultipleSelect(true);
		barcodeList.setVisibleItemCount(10);
		barcodeList.setSize("15em", "202px");

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

		compoundList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String compound = compoundHandler.lastSelected();				
				getDoseLevels(compound, null);
			}
		});

		
		SimplePager exprPager = new SimplePager(TextLocation.CENTER,
				pagerResources, true, 100, true);
		verticalPanel.add(exprPager);

		exprGrid = new DataGrid<ExpressionRow>();
		exprGrid.setStyleName("exprGrid");
		exprGrid.setSize("100%", "500px");
		exprGrid.setPageSize(20);
		exprGrid.setEmptyTableWidget(new HTML("No Data to Display Yet"));

		verticalPanel.add(exprGrid);
		asyncProvider.addDataDisplay(exprGrid);
		exprPager.setDisplay(exprGrid);

		listDataProvider = new ListDataProvider<ExpressionRow>();
		
		compoundHandler.addAfter(doseHandler);
		compoundHandler.addAfter(timeHandler);
		doseHandler.addAfter(barcodeHandler);
		timeHandler.addAfter(barcodeHandler);

		getCompounds();
	}

	private void setupColumns() {
		// todo: explicitly set the width of each column
		NumberCell nc = new NumberCell();

		int count = exprGrid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			exprGrid.removeColumn(0);
		}

		TextColumn<ExpressionRow> probeCol = new TextColumn<ExpressionRow>() {
			public String getValue(ExpressionRow er) {
				return er.getProbe();
			}
		};
		exprGrid.addColumn(probeCol, "Probe");

		TextColumn<ExpressionRow> titleCol = new TextColumn<ExpressionRow>() {
			public String getValue(ExpressionRow er) {
				return er.getTitle();
			}
		};
		exprGrid.addColumn(titleCol, "Title");
		chartTable.removeColumns(0, chartTable.getNumberOfColumns());
		chartTable.addColumn(ColumnType.STRING, "Probe");
		
		int i = 0;
		List<Barcode> selection = barcodeHandler.lastMultiSelection();		
		for (Barcode bc : selection) {
			Column<ExpressionRow, Number> valueCol = new ExpressionColumn(nc, i);
			exprGrid.addColumn(valueCol, bc.getShortTitle());
			chartTable.addColumn(ColumnType.NUMBER, bc.getShortTitle());
			i += 1;
		}
		
	}

	void getCompounds() {
		compoundList.clear();
		owlimService.compounds(compoundHandler.retrieveCallback());
	}

	void getDoseLevels(String compound, String organ) {
		doseLevelList.clear();
		owlimService.doseLevels(null, null, doseHandler.retrieveCallback());
	}

	void getBarcodes(String compound, String organ, String doseLevel,
			String time) {
		barcodeList.clear();
		owlimService.barcodes(compound, organ, doseLevel, time,
				barcodeHandler.retrieveCallback());
	}

	void getTimes(String compound, String organ) {
		timeList.clear();
		owlimService.times(compound, organ, timeHandler.retrieveCallback());
	}

	void getExpressions() {
		setupColumns();
		List<String> codes = new ArrayList<String>();
		for (Barcode code: barcodeHandler.lastMultiSelection()) {
			codes.add(code.getCode());
		}
		
		kcService.loadDataset(codes,
				chosenProbes, chosenValueType, new AsyncCallback<Integer>() {
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
								
				chartTable.removeRows(0, chartTable.getNumberOfRows());
				for (int i = 0; i < result.size(); ++i) {
					chartTable.addRow();
					ExpressionRow row = result.get(i);
					int cols = barcodeHandler.lastMultiSelection().size();
					chartTable.setValue(i, 0, row.getProbe());
					for (int j = 0; j < cols; ++j) {
						chartTable.setValue(i, j + 1, row.getValue(j).getValue());
					}
					exprChart.draw(chartTable);
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
}
