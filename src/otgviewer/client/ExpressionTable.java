package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.ValueType;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.Resources;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;

public class ExpressionTable extends Composite {

	// Visible columns
	private boolean geneIdColVis = false, probeColVis = false,
			probeTitleColVis = true, geneSymColVis = true;
	private String chosenProbe; // single user-selected probe

	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	private DataGrid<ExpressionRow> exprGrid;
	private DoubleBox absValBox;

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	
	public String getChosenProbe() {
		return chosenProbe;
	}
	
	/**
	 * This constructor will be used by the GWT designer. (Not functional at run time)
	 * @wbp.parser.constructor
	 */
	public ExpressionTable() {
		this(null);
	}
	
	public ExpressionTable(MenuBar menuBar) {

		DockPanel dockPanel = new DockPanel();
		dockPanel.setStyleName("none");
		initWidget(dockPanel);
		dockPanel.setSize("100%", "100%");

		exprGrid = new DataGrid<ExpressionRow>();
		dockPanel.add(exprGrid, DockPanel.CENTER);
		exprGrid.setStyleName("exprGrid");
		exprGrid.setPageSize(20);
		exprGrid.setSize("100%", "400px");
		exprGrid.setSelectionModel(new MultiSelectionModel<ExpressionRow>());
		exprGrid.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {
				for (ExpressionRow r: exprGrid.getDisplayedItems()) {
					if (exprGrid.getSelectionModel().isSelected(r)) {
						chosenProbe = r.getProbe();						
					}
				}		
			}
		});
		asyncProvider.addDataDisplay(exprGrid);		
		AsyncHandler colSortHandler = new AsyncHandler(exprGrid);
		

		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		horizontalPanel.setStyleName("colored");
		dockPanel.add(horizontalPanel, DockPanel.NORTH);
		horizontalPanel.setWidth("");

		Resources r = GWT.create(Resources.class);
		SimplePager simplePager = new SimplePager(TextLocation.CENTER,
				r, true, 100, true);
		simplePager.setStyleName("spacedLayout");
		horizontalPanel.add(simplePager);
		simplePager.setDisplay(exprGrid);
		
		Label label = new Label("Absolute value >=");
		label.setStyleName("highlySpaced");
		label.setDirection(Direction.LTR);
		horizontalPanel.add(label);
		label.setWidth("");

		absValBox = new DoubleBox();
		absValBox.setText("0.00");
		horizontalPanel.add(absValBox);

		Button absApply = new Button("Apply");
		horizontalPanel.add(absApply);
		absApply.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent e) {
				// force reload
				getExpressions(null, true);				
			}
		});

		Button absClear = new Button("No filter");
		horizontalPanel.add(absClear);
		absClear.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent e) {
				absValBox.setValue(0.0);
				// force reload
				getExpressions(null, true);				
			}
		});

		exprGrid.addColumnSortHandler(colSortHandler);
		
		setupMenu(menuBar);

	}
	
	private void setupMenu(MenuBar menuBar) {
		MenuBar menuBar_3 = new MenuBar(true);
		
		MenuItem mntmActions_1 = new MenuItem("Actions", false, menuBar_3);
		
		MenuItem mntmDownloadCsv = new MenuItem("Download CSV", false, new Command() {
			public void execute() {
				kcService.prepareCSVDownload(new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to prepare the requested data for download.");
					}
					public void onSuccess(String url) {
						Window.open(url, "_blank", "");
					}
				});
				
			}
		});
		menuBar_3.addItem(mntmDownloadCsv);		
		menuBar.addItem(mntmActions_1);
		MenuBar menuBar_2 = new MenuBar(true);

		MenuItem mntmNewMenu_1 = new MenuItem("New menu", false, menuBar_2);

		MenuItem mntmGeneId = new MenuItem("Gene ID", false, new Command() {
			public void execute() {
				geneIdColVis = !geneIdColVis;
				setupColumns();
			}
		});
		menuBar_2.addItem(mntmGeneId);

		MenuItem mntmProbeName = new MenuItem("Probe ID", false,
				new Command() {
					public void execute() {
						probeColVis = !probeColVis;
						setupColumns();
					}
				});
		menuBar_2.addItem(mntmProbeName);

		MenuItem mntmGeneName = new MenuItem("Probe title", false, new Command() {
			public void execute() {
				probeTitleColVis = !probeTitleColVis;
				setupColumns();
			}
		});
		menuBar_2.addItem(mntmGeneName);
		

		MenuItem mntmGeneSym = new MenuItem("Gene symbol", false, new Command() {
			public void execute() {
				geneSymColVis = ! geneSymColVis;				
				setupColumns();
			}
		});
		menuBar_2.addItem(mntmGeneSym);
		
		mntmNewMenu_1.setHTML("Columns");
		menuBar.addItem(mntmNewMenu_1);
	}

	private int extraCols = 0;
	private void setupColumns() {
		// todo: explicitly set the width of each column
		TextCell tc = new TextCell();

		int count = exprGrid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			exprGrid.removeColumn(0);
		}
		exprGrid.getColumnSortList().clear();

		extraCols = 0;

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
			exprGrid.addColumn(titleCol, "Probe title");
			extraCols += 1;
		}

		if (geneIdColVis) {
			TextColumn<ExpressionRow> geneIdCol = new TextColumn<ExpressionRow>() {
				public String getValue(ExpressionRow er) {
					return arrayString(er.getGeneIds());
				}
			};
			exprGrid.addColumn(geneIdCol, "Gene ID");
			extraCols += 1;
		}

		if (geneSymColVis) {
			TextColumn<ExpressionRow> geneSymCol = new TextColumn<ExpressionRow>() {
				public String getValue(ExpressionRow er) {
					return arrayString(er.getGeneSyms());
				}
			};
			exprGrid.addColumn(geneSymCol, "Gene sym");
			extraCols += 1;
		}

		int i = 0;
		
		for (Barcode bc : selectedBarcodes) {
			Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, i);
			valueCol.setSortable(true);
			exprGrid.addColumn(valueCol, bc.getShortTitle());
			
			if (i == 0 && exprGrid.getColumnSortList().size() == 0) {
				exprGrid.getColumnSortList().push(valueCol);
			}
			i += 1;
		}

		Column<ExpressionRow, String> avgCol = new ExpressionColumn(tc, i);
		avgCol.setSortable(true);
		exprGrid.addColumn(avgCol, "Average");
		i += 1;
				
	}
	
	List<Barcode> selectedBarcodes = new ArrayList<Barcode>();
	public void setSelectedBarcodes(List<Barcode> selection) {
		selectedBarcodes = selection;
	}
	
	DataFilter chosenDataFilter;
	public void setDataFilter(DataFilter filter) {
		chosenDataFilter = filter;
	}
	
	ValueType chosenValueType;
	public void setValueType(ValueType valueType) {
		chosenValueType = valueType;
	}
	
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;

		AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get expression values.");
			}

			public void onSuccess(List<ExpressionRow> result) {
				exprGrid.setRowData(start, result);

//				if (chartTable != null) {
//					chartTable.removeRows(0, chartTable.getNumberOfRows());
//					for (int i = 0; i < result.size(); ++i) {
//						chartTable.addRow();
//						ExpressionRow row = result.get(i);
//						int cols = barcodeHandler.lastMultiSelection().size();
//						chartTable.setValue(i, 0, row.getProbe());
//						for (int j = 0; j < cols; ++j) {
//							chartTable.setValue(i, j + 1, row.getValue(j)
//									.getValue());
//						}
//						
//						exprChart.draw(chartTable);						
//					}
//				}
			}
		};

		protected void onRangeChanged(HasData<ExpressionRow> display) {
			Range range = display.getVisibleRange();
			
			ColumnSortList csl = exprGrid.getColumnSortList();
			boolean asc = false;
			int col = 0;
			if (csl.size() > 0) {
				col = exprGrid.getColumnIndex((Column<ExpressionRow, ?>) csl.get(0).getColumn()) - extraCols;
				asc = csl.get(0).isAscending();				
			}
			start = range.getStart();
			kcService.datasetItems(range.getStart(), range.getLength(), col, asc,						
					rowCallback);
		}

	}
	
	private String arrayString(String[] ss) {
		String r = "";
		for (int i = 0; i < ss.length; ++i) {		
			r += ss[i];
			if (i < ss.length - 1) {
				r += ", ";
			}
		}
		return r;
	}
	
	String[] currentProbes;
	public void getExpressions(String[] displayedProbes, boolean usePreviousProbes) {
		if (!usePreviousProbes) {
			this.currentProbes = displayedProbes;
		}
		
		exprGrid.setRowCount(0, false);
		setupColumns();
		List<DataColumn> cols = new ArrayList<DataColumn>();
		for (Barcode code : selectedBarcodes) {
			cols.add(code);
		}
		cols.add(new Group("Average", selectedBarcodes.toArray(new Barcode[0])));
		
		kcService.loadDataset(chosenDataFilter, cols, currentProbes, chosenValueType,
				absValBox.getValue(),
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

	
	class ExpressionColumn extends Column<ExpressionRow, String> {
		int i;
		TextCell tc;

		public ExpressionColumn(TextCell tc, int i) {
			super(tc);
			this.i = i;
			this.tc = tc;
		}

		public String getValue(ExpressionRow er) {
			if (!er.getValue(i).getPresent()) {
				return "(absent)";
			} else {
				NumberFormat fmt = NumberFormat.getDecimalFormat();				
				return fmt.format(er.getValue(i).getValue());
			}
		}
	}
	
	
	public void resizeInterface(int newHeight) {
		String h3 = (newHeight - exprGrid.getAbsoluteTop() - 45) + "px";
		exprGrid.setHeight(h3);	
	}
}
