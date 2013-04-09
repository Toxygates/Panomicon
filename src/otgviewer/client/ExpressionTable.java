package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.charts.AdjustableChartGrid;
import otgviewer.client.charts.ChartGridFactory;
import otgviewer.client.charts.ChartGridFactory.AChartAcceptor;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.TickMenuItem;
import otgviewer.shared.AType;
import otgviewer.shared.Association;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.Pair;
import otgviewer.shared.SharedUtils;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.PageSizePager;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.Resources;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.Range;

public class ExpressionTable extends DataListenerWidget { //implements RequiresResize, ProvidesResize {

	private final int PAGE_SIZE = 25;
	
	private Screen screen;
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	private DataGrid<ExpressionRow> exprGrid;
	private SimplePager sp;
	private HorizontalPanel tools, analysisTools;
	private DockLayoutPanel dockPanel;
	
	private DoubleBox absValBox;
	private ListBox valueTypeList = new ListBox();
	
	private final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	private final SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	private static otgviewer.client.Resources resources = GWT.create(otgviewer.client.Resources.class);
	
	private List<Synthetic> synthetics = new ArrayList<Synthetic>();
	private List<Column<ExpressionRow, ?>> synthColumns = new ArrayList<Column<ExpressionRow, ?>>();
	private List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
 	private List<AssociationColumn> associationColumns = new ArrayList<AssociationColumn>();
 	
 	private int dataColumns = 0;
 	
	private ListBox groupsel1 = new ListBox();
	private ListBox groupsel2 = new ListBox();
	
	private int highlightedRow = -1;
	private String[] displayedProbes;
//	private List<String> displayedGeneIds = new ArrayList<String>();
	private Map<AType, Association> associations = new HashMap<AType, Association>();
		
	
 	private boolean waitingForAssociations = true, loadedData = false;
 	
 	private Barcode[] chartBarcodes = null;

	public ExpressionTable(Screen _screen) {
		screen = _screen;
//		dockPanel = new DockLayoutPanel(Unit.PX);
		initHideableColumns();
		
		exprGrid = new DataGrid<ExpressionRow>();
//		dockPanel.add(exprGrid);
//		initWidget(dockPanel);
		initWidget(exprGrid);
		
		exprGrid.setStyleName("exprGrid");
		exprGrid.setPageSize(PAGE_SIZE);
		exprGrid.setWidth("100%");

		exprGrid.setSelectionModel(new NoSelectionModel<ExpressionRow>());
		exprGrid.setRowStyles(new RowHighligher());
		
		exprGrid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	
		asyncProvider.addDataDisplay(exprGrid);		
		AsyncHandler colSortHandler = new AsyncHandler(exprGrid);
		
		exprGrid.addColumnSortHandler(colSortHandler);
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
				changeValueType(getValueType());
				getExpressions();
			}
		});

		Resources r = GWT.create(Resources.class);
		sp = new SimplePager(TextLocation.CENTER, r, true, 500, true);
		sp.setStyleName("slightlySpaced");
		horizontalPanel.add(sp);		
		sp.setDisplay(exprGrid);
		
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
		pager.setDisplay(exprGrid);		
		
		Label label = new Label("Magnitude >=");
		label.setStyleName("highlySpaced");		
		horizontalPanel.add(label);

		absValBox = new DoubleBox();
		absValBox.setText("0.00");
		absValBox.setWidth("5em");
		horizontalPanel.add(absValBox);
		absValBox.addValueChangeHandler(new ValueChangeHandler<Double>() {			
			public void onValueChange(ValueChangeEvent<Double> event) {
				refilterData();				
			}
		});
		
		horizontalPanel.add(new Button("Apply", new ClickHandler() {
			public void onClick(ClickEvent e) {
				refilterData();
			}
		}));
		horizontalPanel.add(new Button("No filter", new ClickHandler() {
			public void onClick(ClickEvent e) {
				absValBox.setValue(0.0);
				refilterData();
			}
		}));

		DisclosurePanel analysisDisclosure = new DisclosurePanel("Analysis");
		tools.add(analysisDisclosure);
		analysisDisclosure.addOpenHandler(new OpenHandler<DisclosurePanel>() {			
			@Override
			public void onOpen(OpenEvent<DisclosurePanel> event) {
				screen.showToolbar(analysisTools, 35); //hack for IE8!
			}
		});
		analysisDisclosure.addCloseHandler(new CloseHandler<DisclosurePanel>() {			
			@Override
			public void onClose(CloseEvent<DisclosurePanel> event) {
				screen.hideToolbar(analysisTools);				
			}
		});		
	}
	
	public Widget analysisTools() { return analysisTools; }
	
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
		
		analysisTools.add(new Button("Remove tests", new ClickHandler() {
			public void onClick(ClickEvent ce) {
				if (!synthetics.isEmpty()) {
					for (int i = 0; i < synthColumns.size(); ++i) {						
						Column<ExpressionRow, ?> c = synthColumns.get(i);
						removeColumn(c);
					}
					synthColumns.clear();
					synthetics.clear();							
				}
			}
		}));
		analysisTools.setVisible(false); //initially hidden		
	}
	
	private void addTwoGroupSynthetic(final Synthetic.TwoGroupSynthetic synth, final String name) {
		if (groupsel1.getSelectedIndex() == -1 || groupsel2.getSelectedIndex() == -1) {
			Window.alert("Please select two groups to perform " + name + ".");
		} else if (groupsel1.getSelectedIndex() == groupsel2.getSelectedIndex()) {
			Window.alert("Please select two different groups to perform " + name + ".");
		} else {
			final Group g1 = Utils.findGroup(chosenColumns, groupsel1.getItemText(groupsel1.getSelectedIndex()));
			final Group g2 = Utils.findGroup(chosenColumns, groupsel2.getItemText(groupsel2.getSelectedIndex()));
			synth.setGroups(g1, g2);
			kcService.addTwoGroupTest(synth, new AsyncCallback<Void>() {
				public void onSuccess(Void v) {					
					addSynthColumn(synth);					
					//force reload
					exprGrid.setVisibleRangeAndClearData(exprGrid.getVisibleRange(), true); 
				}
				public void onFailure(Throwable caught) {
					Window.alert("Unable to perform " + name);
				}
			});
		}
	}
	
	MenuItem[] menuItems() {
		MenuItem[] r = new MenuItem[2];
		MenuBar menuBar_3 = new MenuBar(true);
		
		MenuItem mntmActions_1 = new MenuItem("Actions", false, menuBar_3);		
		final DataListenerWidget w = this;
		MenuItem mntmDownloadCsv = new MenuItem("Download CSV...", false, new Command() {
			public void execute() {
				kcService.prepareCSVDownload(new PendingAsyncCallback<String>(w, "Unable to prepare the requested data for download.") {
					
					public void handleSuccess(String url) {
						final String downloadUrl = url;
						final DialogBox db = new DialogBox(false, true);							
												
						db.setHTML("Your download is ready.");				
						HorizontalPanel hp = new HorizontalPanel();
						
						hp.add(new Button("Download", new ClickHandler() {
							public void onClick(ClickEvent ev) {
								Window.open(downloadUrl, "_blank", "");
								db.hide();
							}
						}));
						
						hp.add(new Button("Cancel", new ClickHandler() {
							public void onClick(ClickEvent ev) {
								db.hide();								
							}
						}));
						
						db.add(hp);
						db.setPopupPositionAndShow(Utils.displayInCenter(db));						
					}
				});
				
			}
		});
		menuBar_3.addItem(mntmDownloadCsv);
		
		MenuItem mi = new MenuItem("Export to TargetMine...", false, new Command() {
			public void execute() {
				Utils.displayInPopup("TargetMine export", new GeneExporter(w, exprGrid.getRowCount()));
			}
		});
		
		menuBar_3.addItem(mi);
		
		r[0] = mntmActions_1;
		
		MenuBar menuBar_2 = new MenuBar(true);
		MenuItem mntmNewMenu_1 = new MenuItem("New menu", false, menuBar_2);

		for (final HideableColumn c: hideableColumns) {
			new TickMenuItem(menuBar_2, c.name(), c.visible()) {
				@Override
				public void stateChange(boolean newState) {
					c.setVisibility(newState);	
					if (newState) {
						addExtraColumn(((Column<ExpressionRow, ?>) c), c.name());
						getAssociations();
					} else {
						removeExtraColumn((Column<ExpressionRow, ?>) c);
					}				

				}				
			};
		}
		
		mntmNewMenu_1.setHTML("Columns");
		r[1] = mntmNewMenu_1;
		return r;
	}

	
	private void addExtraColumn(Column<ExpressionRow, ?> col, String name) {
		col.setCellStyleNames("extraColumn");		
		exprGrid.insertColumn(extraCols, col, name);
		extraCols += 1;
	}
	
	private void removeExtraColumn(Column<ExpressionRow, ?> col) {
		exprGrid.removeColumn(col);
		extraCols -= 1;
	}
	
	private void addDataColumn(Column<ExpressionRow, ?> col, String title) {
		col.setSortable(true);
		exprGrid.addColumn(col, title);
		col.setCellStyleNames("dataColumn");		
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
		ToolColumn tcl = new ToolColumn(new ToolCell(this));
		exprGrid.addColumn(tcl, "");
		tcl.setCellStyleNames("clickCell");
		exprGrid.setColumnWidth(tcl, "40px");		
		extraCols += 1;
		
		for (HideableColumn c: hideableColumns) {
			if (c.visible()) {
				Column<ExpressionRow, ?> cc = (Column<ExpressionRow, ?>) c;
				addExtraColumn(cc, c.name());												
			}
		}		

		dataColumns = 0;		
		//columns with data
		for (DataColumn c : chosenColumns) {
			Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, dataColumns);			
			addDataColumn(valueCol, c.getShortTitle());			
			valueCol.setCellStyleNames(((Group) c).getStyleName());
			if (dataColumns == 0 && exprGrid.getColumnSortList().size() == 0) {
				exprGrid.getColumnSortList().push(valueCol); //initial sort
			}
			dataColumns += 1;
		}
		
		for (Synthetic s: synthetics) {
			addSynthColumn(s);			
		}				
	}
	

	private void addSynthColumn(Synthetic s) {
		TextCell tc = new TextCell();
		synthetics.add(s);
		Column<ExpressionRow, String> ttestCol = new ExpressionColumn(tc, dataColumns);
		synthColumns.add(ttestCol);
		exprGrid.addColumn(ttestCol, s.getShortTitle());
		ttestCol.setCellStyleNames("extraColumn");		
		ttestCol.setSortable(true);
		dataColumns += 1;
	}
	
	private void removeColumn(Column<ExpressionRow, ?> c) {
		ColumnSortList csl = exprGrid.getColumnSortList();
		
		for (int i = 0; i < csl.size(); ++i) {
			ColumnSortInfo csi = exprGrid.getColumnSortList().get(i);
			if (csi.getColumn() == c) {
				csl.remove(csi);
				break;
			}
		}		
		exprGrid.removeColumn(c);
		dataColumns -= 1;
	}

	private void initHideableColumns() {
		SafeHtmlCell shc = new SafeHtmlCell();
		
		hideableColumns.add(new LinkingColumn(shc, "Gene ID", false) {
			@Override
			String formLink(String value) {
				return AType.formGeneLink(value);
			}
			@Override
			Collection<Pair<String, String>> getLinkableValues(ExpressionRow er) {
				return Pair.duplicate(Arrays.asList(er.getGeneIds()));
			}						
		});
		
		hideableColumns.add(new DefHideableColumn("Gene Sym", true) {
			public String getValue(ExpressionRow er) {				
				return SharedUtils.mkString(er.getGeneSyms(), ", ");
			}
		});
		hideableColumns.add(new DefHideableColumn("Probe title", true) {
			public String getValue(ExpressionRow er) {				
				return er.getTitle();
			}
		});
		hideableColumns.add(new DefHideableColumn("Probe", true) {
			public String getValue(ExpressionRow er) {				
				return er.getProbe();
			}
		});
		
		
		for (AType at: AType.values()) {
			AssociationColumn ac = new AssociationColumn(shc, at);
			associationColumns.add(ac);
			hideableColumns.add(ac);
		}
		
	}
	
	private AType[] visibleAssociations() {
		List<AType> r = new ArrayList<AType>();
		for (AssociationColumn ac: associationColumns) {
			if (ac.visible()) {
				r.add(ac.assoc);
			}
		}
		return r.toArray(new AType[0]);
	}
	
	private void getAssociations() {
		waitingForAssociations = true;					
		AType[] vas = visibleAssociations();
		if (vas.length > 0) {
			AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
				public void onFailure(Throwable caught) {
					Window.alert("Unable to get associations: " + caught.getMessage());
				}

				public void onSuccess(Association[] result) {
					associations.clear();
					waitingForAssociations = false;
					for (Association a: result) {
						associations.put(a.type(), a);	
					};				
					exprGrid.redraw();
				}
			};

			owlimService.associations(chosenDataFilter, vas,
					displayedProbes, 
					assocCallback);
		}
	}
	
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;
		
		AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get expression values: " + caught.getMessage());
			}

			public void onSuccess(List<ExpressionRow> result) {
				if (result.size() > 0) {
					exprGrid.setRowData(start, result);
					displayedProbes = new String[result.size()];
//					List<String> geneIds = new ArrayList<String>();		
					for (int i = 0; i < displayedProbes.length; ++i) {			
						displayedProbes[i] = result.get(i).getProbe();
//						geneIds.addAll(Arrays.asList(result.get(i).getGeneIds()));
					}		
//					displayedGeneIds = geneIds;
					highlightedRow = -1;							
					getAssociations();
				} else {
					Window.alert("Unable to obtain data. If you have not used Toxygates in a while, try reloading the page.");
				}
			}
		};

		protected void onRangeChanged(HasData<ExpressionRow> display) {
			if (loadedData) {
				Range range = display.getVisibleRange();

				ColumnSortList csl = exprGrid.getColumnSortList();
				boolean asc = false;
				int col = 0;
				if (csl.size() > 0) {
					col = exprGrid
							.getColumnIndex((Column<ExpressionRow, ?>) csl.get(
									0).getColumn())
							- extraCols;
					asc = csl.get(0).isAscending();
				}
				start = range.getStart();
				if (range.getLength() > 0) {
					kcService.datasetItems(range.getStart(), range.getLength(),
							col, asc, rowCallback);
				}
			}
		}

	}

	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		 //invalidate synthetic columns, since they depend on
		//normal columns
		dataColumns -= synthetics.size();
		synthetics.clear();
		
		groupsel1.clear();
		groupsel2.clear();
		for (DataColumn dc: columns) {
			if (dc instanceof Group) {
				groupsel1.addItem(dc.getShortTitle());
				groupsel2.addItem(dc.getShortTitle());
			}
		}
		
		if (columns.size() >= 2) {
			groupsel1.setSelectedIndex(0);
			groupsel2.setSelectedIndex(1);			
		}
		
		chartBarcodes = null;
	}
	
	void refilterData() {
		if (loadedData) {
			setEnabled(false);
			exprGrid.setRowCount(0, false);
			List<DataColumn> cols = new ArrayList<DataColumn>();
			cols.addAll(chosenColumns);
			kcService.refilterData(chosenDataFilter, cols, chosenProbes,
					absValBox.getValue(), synthetics,
					new AsyncCallback<Integer>() {
						public void onFailure(Throwable caught) {
							getExpressions(); //the user probably let the session expire							
						}

						public void onSuccess(Integer result) {
							exprGrid.setRowCount(result);
							exprGrid.setVisibleRangeAndClearData(new Range(0,
									PAGE_SIZE), true);
							setEnabled(true);
						}
					});
		}
	}
	
	public void getExpressions() {
		setEnabled(false);
		exprGrid.setRowCount(0, false);		
		setupColumns();
		List<DataColumn> cols = new ArrayList<DataColumn>();
		cols.addAll(chosenColumns);

		//load data
		kcService.loadDataset(chosenDataFilter, cols, chosenProbes, chosenValueType,
				absValBox.getValue(), synthetics, 
				new AsyncCallback<Integer>() {
					public void onFailure(Throwable caught) {						
						Window.alert("Unable to load dataset");					
					}
					
					public void onSuccess(Integer result) {
						if (result > 0) {
							loadedData = true;
							setEnabled(true);
							exprGrid.setRowCount(result);
							exprGrid.setVisibleRangeAndClearData(new Range(0, PAGE_SIZE),
									true);
						} else {
							Window.alert("No data was available. If you have not used Toxygates for a while, try reloading the page.");
						}
					}
				});		
	}
	
	private void setEnabled(boolean enabled) {
		setEnabled(tools, enabled);
		setEnabled(analysisTools, enabled);
	}
	
	private void setEnabled(HasWidgets root, boolean enabled) {
		for (Widget w: root) {
			if (w instanceof HasWidgets) {
				setEnabled((HasWidgets) w, enabled);
			}
			if (w instanceof FocusWidget) {
				((FocusWidget) w).setEnabled(enabled);
			}
		}
	}
	
	private class RowHighligher implements RowStyles<ExpressionRow> {
		@Override
		public String getStyleNames(ExpressionRow row, int rowIndex) {
			if (highlightedRow != -1 && rowIndex == highlightedRow + exprGrid.getVisibleRange().getStart()) {
				return "highlightedRow";
			} else {
				return "";
			}
		}		
	}
	
	class ExpressionColumn extends Column<ExpressionRow, String> {
		int i;
		TextCell tc;
		NumberFormat df = NumberFormat.getDecimalFormat();
		NumberFormat sf = NumberFormat.getScientificFormat();
		
		public ExpressionColumn(TextCell tc, int i) {
			super(tc);
			this.i = i;
			this.tc = tc;	
		}

		public String getValue(ExpressionRow er) {
			if (!er.getValue(i).getPresent()) {
				return "(absent)";
			} else {	
				return Utils.formatNumber(er.getValue(i).getValue());								
			}
		}
	}
	
	
	class ToolCell extends ImageClickCell {
		
		public ToolCell(DataListenerWidget owner) {
			super(resources.chart());
		}
		
		public void onClick(final String value) {			
			highlightedRow = SharedUtils.indexOf(displayedProbes, value);
			exprGrid.redraw();
			
			final ChartGridFactory cgf = new ChartGridFactory(chosenDataFilter, chosenColumns);
			Utils.ensureVisualisationAndThen(new Runnable() {
				public void run() {
					cgf.makeRowCharts(screen, chartBarcodes, chosenValueType, value, 
							new AChartAcceptor() {
						public void acceptCharts(final AdjustableChartGrid cg) {
							Utils.displayInPopup("Charts", cg, true);							
						}

						public void acceptBarcodes(Barcode[] bcs) {
							chartBarcodes = bcs;
						}
					});			
				}
			});
		}
	}
	
	class ToolColumn extends Column<ExpressionRow, String> {
			
		public ToolColumn(ToolCell tc) {
			super(tc);			
		}
		
		public String getValue(ExpressionRow er) {
			if (er != null) {
				return er.getProbe();
			} else {
				return "";
			}
		}					
	}
	
	
	interface HideableColumn {
		String name();
		boolean visible();
		void setVisibility(boolean v);
	}
	
	abstract class LinkingColumn extends Column<ExpressionRow, SafeHtml> implements HideableColumn {
		private boolean visible;
		SafeHtmlCell c;
		String name;
		public LinkingColumn(SafeHtmlCell c, String name, boolean initState) {
			super(c);
			visible = initState;
			this.name = name;
			this.c = c;
		}
				
		public String name() { return name; }
		public boolean visible() { return this.visible; }
		public void setVisibility(boolean v) { visible = v; }		
		
		protected List<String> makeLinks(Collection<Pair<String, String>> values) {
			List<String> r = new ArrayList<String>();
			for (Pair<String, String> v: values) {
				String l = formLink(v.second());
				if (l != null) {
					r.add("<a target=\"_TGassoc\" href=\"" + l + "\">" + v.first() + "</a>");
				} else {
					r.add(v.first()); //no link
				}				
			}
			return r;
		}
		
		public SafeHtml getValue(ExpressionRow er) {
			SafeHtmlBuilder build = new SafeHtmlBuilder();
			String c = SharedUtils.mkString(makeLinks(getLinkableValues(er))
							, ", ");
			build.appendHtmlConstant(c);
			return build.toSafeHtml();
		}
		
		Collection<Pair<String, String>> getLinkableValues(ExpressionRow er) {
			return new ArrayList<Pair<String, String>>();
		}
		
		abstract String formLink(String value);
		
	}
	
	class AssociationColumn extends LinkingColumn implements HideableColumn {
		AType assoc;		
		
		public AssociationColumn(SafeHtmlCell tc, AType association) {
			super(tc, association.title(), false);
			this.assoc = association;			
		}
		
		String formLink(String value) { return assoc.formLink(value); }
		
		Collection<Pair<String, String>> getLinkableValues(ExpressionRow er) {
			Association a = associations.get(assoc);
			if (a.data().containsKey(er.getProbe())) {
				return a.data().get(er.getProbe());				
			} else {
				String[] geneids = er.getGeneIds();
				Set<Pair<String, String>> all = new HashSet<Pair<String, String>>();
				for (String gi : geneids) {
					if (a.data().containsKey(gi)) {
						all.addAll(a.data().get(gi));
					}
				}
				return all;				
			}
		}
		
		public SafeHtml getValue(ExpressionRow er) {		
			SafeHtmlBuilder build = new SafeHtmlBuilder();
			if (waitingForAssociations) {
				build.appendEscaped("(Waiting for data...)");
				return build.toSafeHtml();
			} else {
				if (associations.containsKey(assoc)) {
					return super.getValue(er);					
				} else {
					build.appendEscaped("(Data unavailable)");
				}
			}
			return build.toSafeHtml();
		}		
	}
	
	
	abstract class DefHideableColumn extends TextColumn<ExpressionRow> implements HideableColumn {
		private boolean visible;
		public DefHideableColumn(String name, boolean initState) {
			super();
			visible = initState;
			_name = name;
		}
		
		private String _name;
		public String name() { return _name; }
		public boolean visible() { return this.visible; }
		public void setVisibility(boolean v) { visible = v; }		
	}
	
}
