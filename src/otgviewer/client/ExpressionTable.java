package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import otgviewer.client.charts.AdjustableChartGrid;
import otgviewer.client.charts.ChartGridFactory;
import otgviewer.client.charts.ChartGridFactory.AChartAcceptor;
import otgviewer.client.components.AssociationTable;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ExpressionColumn;
import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.dialog.DialogPosition;
import otgviewer.client.dialog.FilterEditor;
import otgviewer.client.rpc.MatrixService;
import otgviewer.client.rpc.MatrixServiceAsync;
import otgviewer.shared.AType;
import otgviewer.shared.Group;
import otgviewer.shared.ManagedMatrixInfo;
import otgviewer.shared.OTGSample;
import otgviewer.shared.OTGUtils;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.sample.DataColumn;
import t.common.shared.sample.ExpressionRow;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.PageSizePager;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.Resources;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.Range;

/**
 * The main data display table. This class has many different functionalities.
 * It requests microarray expression data dynamically, displays it, 
 * as well as displaying additional dynamic data. It also provides functionality for chart popups.
 * It also has an interface for adding and removing t-tests and u-tests, which can be hidden and 
 * displayed on demand.
 * 
 * Hideable columns and clickable icons are handled by the RichTable superclass.
 * Dynamic (association) columns are handled by the AssociationTable superclass.
 * 
 * @author johan
 *
 */
public class ExpressionTable extends AssociationTable<ExpressionRow> { 

	/**
	 * Initial number of items to show per page at a time (but note that this number can be adjusted by 
	 * the user in the 0-100 range)
	 */
	private final int PAGE_SIZE = 25;
	
	private Screen screen;
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	
	private HorizontalPanel tools, analysisTools;
	//We enable/disable this button when the value type changes
	private Button foldChangeBtn = new Button("Add fold-change difference");
	
	private ListBox valueTypeList = new ListBox();
	
	private final MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
			.create(MatrixService.class);	
	private static otgviewer.client.Resources resources = GWT.create(otgviewer.client.Resources.class);
	
	/**
	 * "Synthetic" columns are tests columns such as t-test and u-test.
	 */
	private List<Synthetic> synthetics = new ArrayList<Synthetic>();
	private List<Column<ExpressionRow, ?>> synthColumns = new ArrayList<Column<ExpressionRow, ?>>();
	
	/**
	 * For selecting sample groups to apply t-test/u-test to
	 */
	private ListBox groupsel1 = new ListBox(), groupsel2 = new ListBox();
	
	/**
	 * Names of the microarray probes currently displayed
	 */
	private String[] displayedProbes;

 	private boolean loadedData = false;
 	private ManagedMatrixInfo matrixInfo = null;
 	
 	private OTGSample[] chartBarcodes = null;

 	private DialogBox filterDialog = null;
 	
 	private final Logger logger = Utils.getLogger("expressionTable");
 	
	public ExpressionTable(Screen _screen) {
		super();
		screen = _screen;
		
		grid.setStyleName("exprGrid");
		grid.setPageSize(PAGE_SIZE);
		
		grid.setSelectionModel(new NoSelectionModel<ExpressionRow>());		
		grid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);	
		asyncProvider.addDataDisplay(grid);		

		makeTools();
		makeAnalysisTools();
		setEnabled(false);
	}
	
	private ValueType getValueType() {
		String vt = valueTypeList.getItemText(valueTypeList
				.getSelectedIndex());
		return ValueType.unpack(vt);		
	}
	
	public Widget tools() { return this.tools; }
	
	/**
	 * Enable or disable the GUI
	 * @param enabled
	 */
	private void setEnabled(boolean enabled) {
		Utils.setEnabled(tools, enabled);
		Utils.setEnabled(analysisTools, enabled);
		enableFoldChangeUI(enabled);		
	}
	
	private void enableFoldChangeUI(boolean enabled) {
		switch (chosenValueType) {
		case Absolute:
			foldChangeBtn.setEnabled(false);
			break;
		case Folds: 
			foldChangeBtn.setEnabled(true && enabled);
			break;
		}		
	}
	
	@Override
	protected void changeValueType(ValueType type) {
		super.changeValueType(type);
		enableFoldChangeUI(true);		
	}
	
	/**
	 * The main (navigation) tool panel
	 */
	private void makeTools() {
		tools = Utils.mkHorizontalPanel();		
		
		HorizontalPanel horizontalPanel = Utils.mkHorizontalPanel(true);		
		horizontalPanel.setStyleName("colored");
		tools.add(horizontalPanel);
		
		valueTypeList.addItem(ValueType.Folds.toString());
		valueTypeList.addItem(ValueType.Absolute.toString());
		changeValueType(ValueType.Folds);
		valueTypeList.setVisibleItemCount(1);
		horizontalPanel.add(valueTypeList);
		valueTypeList.addChangeHandler(new ChangeHandler() {			
			@Override
			public void onChange(ChangeEvent event) {
				removeTests();
				changeValueType(getValueType());
				getExpressions();
			}
		});

		Resources r = GWT.create(Resources.class);

		SimplePager sp = new SimplePager(TextLocation.CENTER, r, true, 500, true);
		sp.setStyleName("slightlySpaced");
		horizontalPanel.add(sp);		
		sp.setDisplay(grid);
		
		PageSizePager pager = new PageSizePager(25) {
			@Override
			protected void onRangeOrRowCountChanged() {
				super.onRangeOrRowCountChanged();
				if (getPageSize() > 100) {
					setPageSize(100);					
				}				
			}			
		};
		
		pager.setStyleName("slightlySpaced");
		horizontalPanel.add(pager);
		pager.setDisplay(grid);			
	}
	
	public Widget analysisTools() { return analysisTools; }
	
	private void removeTests() {
		if (!synthetics.isEmpty()) {
			removeSyntheticColumnsLocal();					
			matrixService.removeTwoGroupTests(new AsyncCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
					Window.alert("There was an error removing the test columns.");					
				}

				@Override
				public void onSuccess(Void result) {
				}				
			});
		}
	}
	
	/**
	 * The tool panel for controlling t-tests and u-tests
	 */
	private void makeAnalysisTools() {
		analysisTools = Utils.mkHorizontalPanel(true);
		analysisTools.setStyleName("colored2");
		
		analysisTools.add(groupsel1);
		groupsel1.setVisibleItemCount(1);
		analysisTools.add(groupsel2);
		groupsel2.setVisibleItemCount(1);
		
		
		analysisTools.add(new Button("Add T-Test", new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.TTest(null, null), "T-Test"); }							
		}));
		
		analysisTools.add(new Button("Add U-Test", new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.UTest(null, null), "U-Test"); }							
		}));
		
		foldChangeBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.MeanDifference(null, null), "Fold-change difference"); }
		});
		analysisTools.add(foldChangeBtn);
		
		analysisTools.add(new Button("Remove tests", new ClickHandler() {
			public void onClick(ClickEvent ce) {
				removeTests();				
			}
		}));
		analysisTools.setVisible(false); //initially hidden		
	}
	
	private static String selectedGroup(ListBox groupSelector) {
		return groupSelector.getItemText(groupSelector.getSelectedIndex());
	}
	
	private void addTwoGroupSynthetic(final Synthetic.TwoGroupSynthetic synth, final String name) {
		if (groupsel1.getSelectedIndex() == -1 || groupsel2.getSelectedIndex() == -1) {
			Window.alert("Please select two groups to compute " + name + ".");
		} else if (groupsel1.getSelectedIndex() == groupsel2.getSelectedIndex()) {
			Window.alert("Please select two different groups to perform " + name + ".");
		} else {
			final Group g1 = OTGUtils.findGroup(chosenColumns, selectedGroup(groupsel1));
			final Group g2 = OTGUtils.findGroup(chosenColumns, selectedGroup(groupsel2));
			synth.setGroups(g1, g2);
			matrixService.addTwoGroupTest(synth, new AsyncCallback<Void>() {
				public void onSuccess(Void v) {							
					addSynthColumn(synth, synth.getShortTitle(screen.schema()), synth.getTooltip());					
					//force reload
					grid.setVisibleRangeAndClearData(grid.getVisibleRange(), true); 
				}
				public void onFailure(Throwable caught) {
					Window.alert("Unable to compute " + name);
				}
			});
		}
	}

	public void downloadCSV() {		
		matrixService.prepareCSVDownload(new PendingAsyncCallback<String>(this, 
				"Unable to prepare the requested data for download.") {
			
			public void handleSuccess(String url) {
				Utils.urlInNewWindow("Your download is ready.", "Download", url);					
			}
		});
	}

	protected void setupColumns() {
		super.setupColumns();
		TextCell tc = new TextCell();
				
		for (int i = 0; i < matrixInfo.numDataColumns(); ++i) {			
			Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, dataColumns);
			valueCol.setDefaultSortAscending(false);
			addDataColumn(valueCol, matrixInfo.columnName(i), matrixInfo.columnHint(i));
			Group g = matrixInfo.columnGroup(i);
			if (g != null) {
				valueCol.setCellStyleNames(g.getStyleName());
			}
		}		

		int i = matrixInfo.numDataColumns();
		for (Synthetic s: synthetics) {
			addSynthColumn(s, matrixInfo.columnName(i), matrixInfo.columnHint(i));
			i++;
		}				
	}
	
	@Override
	protected Column<ExpressionRow, String> toolColumn(Cell<String> cell) {
		return new Column<ExpressionRow, String>(cell) {
			public String getValue(ExpressionRow er) {
				if (er != null) {
					return er.getProbe();
				} else {
					return "";
				}
			}
		};
	}
	
	@Override
	protected Cell<String> toolCell() {
		return new ToolCell(this);
	}

	// TODO remove synthetic bookkeeping from this class, now done on server side
	private void addSynthColumn(Synthetic s, String title, String tooltip) {
		TextCell tc = new TextCell();
		synthetics.add(s);
		Column<ExpressionRow, String> synCol = new ExpressionColumn(tc, dataColumns);
		synthColumns.add(synCol);
		synCol.setDefaultSortAscending(s.isDefaultSortAscending());
		addDataColumn(synCol, title, tooltip);		
		synCol.setCellStyleNames("extraColumn");				
	}
	
	private void removeSyntheticColumnsLocal() {
		for (int i = 0; i < synthColumns.size(); ++i) {						
			Column<ExpressionRow, ?> c = synthColumns.get(i);
			removeDataColumn(c);
		}
		synthColumns.clear();
		synthetics.clear();		
	}
	
	@Override
	protected Header<SafeHtml> getColumnHeader(int column, SafeHtml safeHtml) {
		if (column >= numExtraColumns()) {
			// filterable column
			return new FilteringHeader(safeHtml);
		} else {
			return super.getColumnHeader(column, safeHtml);
		}		
	}
	
	@Override
	protected boolean interceptGridClick(String target, int x, int y) {
		/**
		 * To prevent unwanted interactions between the sorting system and the
		 * filtering system, we have to intercept click events at this high level
		 * and choose whether to pass them (non-filter clicks) on or not 
		 * (filter clicks).
		 */
		
		// Identify a click on the filter image.
		// TODO use a more robust identification method (!!)
		boolean isFilterClick = ((target.startsWith("<img") || target.startsWith("<IMG"))
				&& 
				(target.indexOf("width:12") != -1 || //most browsers
				 target.indexOf("WIDTH: 12") != -1 || // IE9
				 target.indexOf("width: 12") != -1)); // IE8
		if (isFilterClick) {
			// Identify the column that was filtered.
			int col = columnAt(x);			
			int realCol = col - numExtraColumns();
			editColumnFilter(realCol);			
		}
		// If we return true, the click will be passed on to the other widgets
		return !isFilterClick;
	}
	
	protected void editColumnFilter(int column) {
		FilterEditor fe = new FilterEditor(
				matrixInfo.columnName(column),
				column,
				matrixInfo.isUpperFiltering(column),
				matrixInfo.columnFilter(column)) {
			
			@Override
			protected void onChange(Double newVal) {
				applyColumnFilter(editColumn, newVal);
			}			
		};
		filterDialog = Utils.displayInPopup("Edit filter", fe, DialogPosition.Center);				
	}
	
	protected void applyColumnFilter(final int column, final Double filter) {
		setEnabled(false);
		matrixService.setColumnThreshold(column, filter, new AsyncCallback<ManagedMatrixInfo>() {
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("An error occurred when the column filter was changed.");
				filterDialog.setVisible(false);
				setEnabled(true);
			}

			@Override
			public void onSuccess(ManagedMatrixInfo result) {
				if (result.numRows() == 0 && filter != null) {
					Window.alert("No rows match the selected filter. The filter will be reset.");
					applyColumnFilter(column, null);					
				} else {
					setMatrix(result);
					filterDialog.setVisible(false);
				}
			}
		});
	}
	
	protected List<HideableColumn> initHideableColumns() {
		SafeHtmlCell shc = new SafeHtmlCell();
		List<HideableColumn> r = new ArrayList<HideableColumn>();
		
		r.add(new LinkingColumn<ExpressionRow>(shc, "Gene ID", false) {
			@Override
			protected String formLink(String value) {
				return AType.formGeneLink(value);
			}
			@Override
			protected Collection<Pair<String, String>> getLinkableValues(ExpressionRow er) {
				return Pair.duplicate(Arrays.asList(er.getGeneIds()));
			}						
		});
		
		r.add(new DefHideableColumn<ExpressionRow>("Gene Sym", true) {
			public String safeGetValue(ExpressionRow er) {					
				return SharedUtils.mkString(er.getGeneSyms(), ", ");
			}
		});
		r.add(new DefHideableColumn<ExpressionRow>("Probe title", true) {
			public String safeGetValue(ExpressionRow er) {				
				return er.getTitle();
			}
		});
		r.add(new DefHideableColumn<ExpressionRow>("Probe", true) {
			public String safeGetValue(ExpressionRow er) {				
				return er.getProbe();
			}
		});		
		
		//We want gene sym, probe title etc. to be before the association columns going left to right
		r.addAll(super.initHideableColumns());
		
		return r;
	}
	
	public String[] displayedProbes() { return displayedProbes; }
	protected String probeForRow(ExpressionRow row) { return row.getProbe(); }
	protected String[] geneIdsForRow(ExpressionRow row) { return row.getGeneIds(); }
	
	/**
	 * This class fetches data on demand when the user requests a different page.
	 * Data must first be loaded with getExpressions.
	 * @author johan
	 *
	 */
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private Range range;
		final String errMsg = "Unable to obtain data. If you have not used Toxygates in a while, try reloading the page.";
		AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				loadedData = false;
				Window.alert(errMsg);
			}

			public void onSuccess(List<ExpressionRow> result) {
				if (result.size() > 0) {
					updateRowData(range.getStart(), result);
					displayedProbes = new String[result.size()];					
					
					for (int i = 0; i < displayedProbes.length; ++i) {			
						displayedProbes[i] = result.get(i).getProbe();
					}		

					highlightedRow = -1;							
					getAssociations();
				} else {
					Window.alert(errMsg);
				}
			}
		};

		protected void onRangeChanged(HasData<ExpressionRow> display) {
			if (loadedData) {
				range = display.getVisibleRange();						
				computeSortParams();
				if (range.getLength() > 0) {
					matrixService.datasetItems(range.getStart(), range.getLength(),
							sortDataColumnIdx(), sortAscending(), rowCallback);
				}
			}
		}
	}

	@Override
	public void columnsChanged(List<Group> columns) {
		HashSet<Group> oldColumns = new HashSet<Group>(chosenColumns);
		HashSet<Group> newColumns = new HashSet<Group>(columns);
		if (newColumns.equals(oldColumns) && newColumns.size() > 0) {
			logger.info("Ignoring column change signal");
			return;
		}
		
		super.columnsChanged(columns);
		 //invalidate synthetic columns, since they depend on
		//normal columns
		removeSyntheticColumnsLocal();
		
		groupsel1.clear();
		groupsel2.clear();
		DataSchema schema = screen.schema();
		for (DataColumn<?> dc: columns) {
			if (dc instanceof Group) {
				groupsel1.addItem(dc.getShortTitle(schema));
				groupsel2.addItem(dc.getShortTitle(schema));
			}
		}
		
		if (columns.size() >= 2) {
			groupsel1.setSelectedIndex(0);
			groupsel2.setSelectedIndex(1);			
		}
		
		chartBarcodes = null;
		loadedData = false;
		
		logger.info("Columns changed (" + columns.size() + ")");
	}
	
	/**
	 * Filter data that has already been loaded
	 */
	void refilterData() {
		if (!loadedData) {
			logger.info("Request to refilter but data was not loaded");
			return;
		}
		setEnabled(false);
		asyncProvider.updateRowCount(0, false);
//		grid.setRowCount(0, false);
		logger.info("Refilter for " + chosenProbes.length + " probes");
		matrixService.selectProbes(chosenProbes, dataUpdateCallback());			
	}
	
	private AsyncCallback<ManagedMatrixInfo> dataUpdateCallback() {
		return new AsyncCallback<ManagedMatrixInfo>() {
			public void onFailure(Throwable caught) {
				getExpressions(); // the user probably let the session
									// expire
			}

			public void onSuccess(ManagedMatrixInfo result) {
				setMatrix(result);			
			}
		};
	}
	
	protected void setMatrix(ManagedMatrixInfo matrix) {
		matrixInfo = matrix;
		asyncProvider.updateRowCount(matrix.numRows(), true);
		int displayRows = (matrix.numRows() > PAGE_SIZE) ? PAGE_SIZE : matrix.numRows();
		grid.setVisibleRangeAndClearData(new Range(0, displayRows), true);
		setEnabled(true);
	}
	
	/**
	 * Load data (when there is nothing stored in our server side session)
	 */
	public void getExpressions() {
		setEnabled(false);
		asyncProvider.updateRowCount(0, false);

		logger.info("begin loading data for " + chosenColumns.size() + " columns and " +
				chosenProbes.length + " probes");
		// load data
		matrixService.loadDataset(chosenColumns, chosenProbes,
				chosenValueType, synthetics,
				new AsyncCallback<ManagedMatrixInfo>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to load dataset");
					}

					public void onSuccess(ManagedMatrixInfo result) {
						if (result.numRows() > 0) {							
							matrixInfo = result;
							loadedData = true;
							setupColumns();
							setMatrix(result);
							
							logger.info("Data successfully loaded");
						} else {
							Window.alert("No data was available. If you have not used " +
									"Toxygates for a while, try reloading the page.");
						}
					}
				});
	}
		
	/**
	 * This cell displays an image that can be clicked to display charts.
	 * @author johan
	 */
	class ToolCell extends ImageClickCell.StringImageClickCell {
		
		public ToolCell(DataListenerWidget owner) {
			super(resources.chart(), false);
		}
		
		public void onClick(final String value) {			
			highlightedRow = SharedUtils.indexOf(displayedProbes, value);
			grid.redraw();
			
			//TODO this will not work for multi-class groups
			SampleClass sc = chosenColumns.get(0).getSamples()[0].
					sampleClass().asMacroClass(screen.schema());		
			logger.info("Create charts for " + sc.toString());
			
			//TODO
			final ChartGridFactory cgf = new ChartGridFactory(sc, screen.schema(), chosenColumns);
			Utils.ensureVisualisationAndThen(new Runnable() {
				public void run() {
					cgf.makeRowCharts(screen, chartBarcodes, chosenValueType, value, 
							new AChartAcceptor() {
						public void acceptCharts(final AdjustableChartGrid cg) {
							Utils.displayInPopup("Charts", cg, true, DialogPosition.Side);							
						}

						public void acceptBarcodes(OTGSample[] bcs) {
							chartBarcodes = bcs;
						}
					});			
				}
			});
		}
	}
	
	class FilterCell extends ImageClickCell.SafeHtmlImageClickCell {
		public FilterCell() {
			super(resources.filter(), true);
		}
		
		public void onClick(SafeHtml value) {
			/*
			 * The filtering mechanism is not handled here, but in
			 * ExpressionTable.interceptGridClick.
			 */ 
		}
	}
	
	class FilteringHeader extends Header<SafeHtml> {
		private SafeHtml value;
		public FilteringHeader(SafeHtml value) {
			super(new FilterCell());
			this.value = value;
		}

		@Override
		public SafeHtml getValue() {
			return value;
		}	
	}
}
