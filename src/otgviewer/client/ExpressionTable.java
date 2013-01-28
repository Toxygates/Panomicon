package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.TickMenuItem;
import otgviewer.shared.Association;
import otgviewer.shared.DataColumn;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.Series;
import otgviewer.shared.SharedUtils;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
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
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
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
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.Range;

public class ExpressionTable extends DataListenerWidget implements RequiresResize, ProvidesResize {

	private final int PAGE_SIZE = 50;
	
	private Screen screen;
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	private DataGrid<ExpressionRow> exprGrid;
	private SimplePager sp;
	private HorizontalPanel tools, analysisTools;
	private DockLayoutPanel dockPanel;
	
	private DoubleBox absValBox;
	private ListBox valueTypeList = new ListBox();
	
	private VerticalPanel seriesChartPanel = new VerticalPanel();	
	private List<SeriesChart> seriesCharts = new ArrayList<SeriesChart>();
	private SeriesChart.Controller chartController;
	
	private final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	private static otgviewer.client.Resources resources = GWT.create(otgviewer.client.Resources.class);
	
	private List<Synthetic> synthColumns = new ArrayList<Synthetic>();
	
	private ListBox groupsel1 = new ListBox();
	private ListBox groupsel2 = new ListBox();
	
	private int highlightedRow = -1;
	private String[] displayedProbes;
	private Map<String, Association> associations = new HashMap<String, Association>();
	private final static String[] expectedAssociations = new String[] { "KEGG pathways", "MF GO terms", 
		"CC GO terms", "BP GO terms", "CHEMBL targets", "DrugBank targets", "UniProt proteins", "Homologene entries" };	
	private List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
 	private List<AssociationColumn> associationColumns = new ArrayList<AssociationColumn>();
 	private boolean waitingForAssociations = true;
 	private Widget toolPanel;
 	
	public ExpressionTable(Screen _screen) {
		screen = _screen;
		dockPanel = new DockLayoutPanel(Unit.EM);
		initHideableColumns();
		
		exprGrid = new DataGrid<ExpressionRow>();
		dockPanel.add(exprGrid);
		initWidget(dockPanel);
		
		exprGrid.setStyleName("exprGrid");
		exprGrid.setPageSize(PAGE_SIZE);
		exprGrid.setWidth("100%");

		exprGrid.setSelectionModel(new NoSelectionModel());
		exprGrid.setRowStyles(new RowHighligher());
		
		exprGrid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	
		asyncProvider.addDataDisplay(exprGrid);		
		AsyncHandler colSortHandler = new AsyncHandler(exprGrid);
		
		exprGrid.addColumnSortHandler(colSortHandler);
		makeTools();
		setEnabled(false);

	}
	
	private ValueType getValueType() {
		String vt = valueTypeList.getItemText(valueTypeList
				.getSelectedIndex());
		return ValueType.unpack(vt);		
	}
	
	public Widget tools() {
		return this.tools;
	}
	
	private boolean warnedPageSize = false;
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
		sp = new SimplePager(TextLocation.CENTER, r, true, 10 * PAGE_SIZE, true);
		sp.setStyleName("slightlySpaced");
		horizontalPanel.add(sp);		
		sp.setDisplay(exprGrid);
		
		
		PageSizePager pager = new PageSizePager(50) {
			@Override
			protected void onRangeOrRowCountChanged() {
				super.onRangeOrRowCountChanged();
				if (getPageSize() > 100 && !warnedPageSize) {
					Window.alert("You are now displaying " + getPageSize() + " rows. Dynamic columns will " + 
							"only be loaded for the first 100 rows on each page.");
					warnedPageSize = true;
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
				analysisTools.setVisible(true);
				screen.deferredResize();				
			}
		});
		analysisDisclosure.addCloseHandler(new CloseHandler<DisclosurePanel>() {			
			@Override
			public void onClose(CloseEvent<DisclosurePanel> event) {
				analysisTools.setVisible(false);
				screen.deferredResize();
			}
		});		
	}
	
	public Widget analysisTools() {
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
				if (!synthColumns.isEmpty()) {
					synthColumns.clear();
					//We have to reload the data to get rid of the synth columns
					//in our server side session (TODO, avoid this)
					getExpressions();	
				}
			}
		}));
		analysisTools.setVisible(false); //initially hidden
		return analysisTools;
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
					synthColumns.add(synth);
					setupColumns();
					exprGrid.setVisibleRangeAndClearData(new Range(0, PAGE_SIZE), true);
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
				Utils.displayInPopup(new GeneExporter(w));
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
					setupColumns();
				}				
			};
		}
		
		mntmNewMenu_1.setHTML("Columns");
		r[1] = mntmNewMenu_1;
		return r;
	}

	
	private void addExtraColumn(Column<ExpressionRow, ?> col, String name) {
		col.setCellStyleNames("extraColumn");
		exprGrid.addColumn(col, name);
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
		exprGrid.setColumnWidth(tcl, "40px");		
		extraCols += 1;
		
		for (HideableColumn c: hideableColumns) {
			if (c.visible()) {
				Column<ExpressionRow, ?> cc = (Column<ExpressionRow, ?>) c;
				addExtraColumn(cc, c.name());								
				extraCols += 1;				
			}
		}		

		int i = 0;		
		//columns with data
		for (DataColumn c : chosenColumns) {
			Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, i);			
			addDataColumn(valueCol, c.getShortTitle());			
			if (i == 0 && exprGrid.getColumnSortList().size() == 0) {
				exprGrid.getColumnSortList().push(valueCol);
			}
			i += 1;
		}
		
		for (Synthetic s: synthColumns) {
			Column<ExpressionRow, String> ttestCol = new ExpressionColumn(tc, i);
			addExtraColumn(ttestCol, s.getShortTitle());
			ttestCol.setSortable(true);
			i += 1;
		}				
	}

	private void initHideableColumns() {
		hideableColumns.add(new DefHideableColumn("Gene ID", false) {
			public String getValue(ExpressionRow er) {
				return arrayString(er.getGeneIds(), ", ");
			}
		});
		hideableColumns.add(new DefHideableColumn("Gene Sym", true) {
			public String getValue(ExpressionRow er) {				
				return arrayString(er.getGeneSyms(), ", ");
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
		
		TextCell tc = new TextCell();
		for (String assoc: expectedAssociations) {
			AssociationColumn ac = new AssociationColumn(tc, assoc);
			associationColumns.add(ac);
			hideableColumns.add(ac);
		}
	}
	
	/**
	 * Return an array of length at most 100, for association retrieval.
	 * @param data
	 * @return
	 */
	private String[] limitLength(String[] data) {
		if (data.length <= 100) {
			return data;
		}
		String[] r = new String[100];
		for (int i = 0; i < 100; ++i) {
			r[i] = data[i];
		}
		return r;		
	}
	
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;

		AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get associations: " + caught.getMessage());
			}
			
			public void onSuccess(Association[] result) {
				associations.clear();
				waitingForAssociations = false;
				for (Association a: result) {
					associations.put(a.title(), a);	
				};				
				exprGrid.redraw();
			}
		};
		
		AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get expression values: " + caught.getMessage());
			}

			public void onSuccess(List<ExpressionRow> result) {
				if (result.size() > 0) {
					exprGrid.setRowData(start, result);
					displayedProbes = new String[result.size()];
					List<String> geneIds = new ArrayList<String>();
					highlightedRow = -1;
					for (int i = 0; i < displayedProbes.length; ++i) {
						displayedProbes[i] = result.get(i).getProbe();
						geneIds.addAll(Arrays.asList(result.get(i).getGeneIds()));
					}						
					waitingForAssociations = true;					
					owlimService.associations(chosenDataFilter, limitLength(displayedProbes), 
							limitLength(geneIds.toArray(new String[0])), assocCallback);
				}
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
	
	private String arrayString(String[] ss, String sep) {
		StringBuilder r = new StringBuilder();
		
		for (int i = 0; i < ss.length; ++i) {		
			r.append(ss[i]);
			if (i < ss.length - 1) {
				r.append(sep);
			}
		}
		return r.toString();
	}

	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		 //invalidate synthetic columns, since they depend on
		//normal columns
		synthColumns.clear();
		
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
	}
	
	public void beginLoading() {
		exprGrid.setRowCount(0, false);		
	}
	
	void refilterData() {
		setEnabled(false);
		exprGrid.setRowCount(0, false);
		List<DataColumn> cols = new ArrayList<DataColumn>();
		cols.addAll(chosenColumns);
		kcService.refilterData(chosenDataFilter, cols, chosenProbes,
				absValBox.getValue(), synthColumns, 
				new AsyncCallback<Integer>() {
					public void onFailure(Throwable caught) {						
						Window.alert("Unable to load dataset");					
					}

					public void onSuccess(Integer result) {
						exprGrid.setRowCount(result);
						exprGrid.setVisibleRangeAndClearData(new Range(0, PAGE_SIZE),
								true);
						setEnabled(true);
					}
				});
	}
	
	public void getExpressions() {
		setEnabled(false);
		exprGrid.setRowCount(0, false);		
		setupColumns();
		List<DataColumn> cols = new ArrayList<DataColumn>();
		cols.addAll(chosenColumns);

		//set up the series charts		
		seriesChartPanel.clear();
		seriesCharts.clear();
		
		chartController  = new SeriesChart.Controller();
		this.addListener(chartController);
		this.propagateTo(chartController);
		seriesChartPanel.add(chartController);
		
		for (DataColumn c: cols) {
			Label l = new Label("Compounds in '" + c.getShortTitle() +"'");
			seriesChartPanel.add(l);			
			l.setStyleName("heading");
			for (String com : c.getCompounds()) {
				SeriesChart sc = new SeriesChart(screen);
				chartController.addChart(sc);				
				seriesChartPanel.add(sc);
				seriesCharts.add(sc);
				this.propagateTo(sc);
				sc.compoundChanged(com);
			}			
		}
		
		//load data
		kcService.loadDataset(chosenDataFilter, cols, chosenProbes, chosenValueType,
				absValBox.getValue(), synthColumns, 
				new AsyncCallback<Integer>() {
					public void onFailure(Throwable caught) {						
						Window.alert("Unable to load dataset");					
					}

					public void onSuccess(Integer result) {
						setEnabled(true);
						exprGrid.setRowCount(result);
						exprGrid.setVisibleRangeAndClearData(new Range(0, PAGE_SIZE),
								true);
					}
				});
	}
	
	private void setEnabled(boolean enabled) {
		setEnabled(tools, enabled);		
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
	
	interface HideableColumn {
		String name();
		boolean visible();
		void setVisibility(boolean v);
	}
	
	class AssociationColumn extends Column<ExpressionRow, String> implements HideableColumn {
		String assoc;
		TextCell tc;
		boolean visible = false;
		
		public AssociationColumn(TextCell tc, String association) {
			super(tc);
			this.assoc = association;
			this.tc = tc;
		}
		
		public String getValue(ExpressionRow er) {		
			if (waitingForAssociations) {
				return "(Waiting for data...)";
			} else {
				if (associations.containsKey(assoc)) {
					Association a = associations.get(assoc);
					if (a.data().containsKey(er.getProbe())) {
						return arrayString(
								a.data().get(er.getProbe())
										.toArray(new String[0]), ", ");
					} else {
						String[] geneids = er.getGeneIds();
						Set<String> all = new HashSet<String>();
						for (String gi : geneids) {
							if (a.data().containsKey(gi)) {
								all.addAll(a.data().get(gi));
							}
						}
						return arrayString(all.toArray(new String[0]), ", ");
					}
				} else {
					return "(Data unavailable)";
				}
			}
		}
		
		public String name() { return assoc; }
		public void setVisibility(boolean v) { visible = v; }		
		public boolean visible() { return this.visible; }		
	}
	
	class ToolCell extends ImageClickCell {
		
		public ToolCell(DataListenerWidget owner) {
			super(resources.chart());
		}
		
		//TODO the popup chart code could be cleaned up/factored out quite a bit
		public void onClick(String value) {			
			highlightedRow = SharedUtils.indexOf(displayedProbes, value);
			exprGrid.redraw();
			
			int chartHeight = 200;
			final int numCharts = seriesCharts.size();

//			int height = chartHeight * numCharts;			
			for (int i = 0; i < numCharts; i++) {
				SeriesChart seriesChart = (SeriesChart) seriesCharts.get(i);
				seriesChart.changeProbe(value);
				seriesChart.setWidth("500px");
				seriesChart.setPixelHeight(chartHeight);
			}
			chartController.redraw();

			HorizontalPanel hp = new HorizontalPanel();
			TabPanel tp = new TabPanel();
			hp.add(tp);
			tp.add(seriesChartPanel, "Individual samples");			
			
			VerticalPanel v = new VerticalPanel();			
			tp.add(v, "Average time series");			
			tp.selectTab(0);
			
			for (DataColumn dc: chosenColumns) {
				Label l = new Label("Compounds in '" + dc.getShortTitle() +"'");
				l.setStyleName("heading");
				v.add(l);
				final SimplePanel sp = new SimplePanel();				
				v.add(sp);
				kcService.getSeries(chosenDataFilter, new String[] { value }, 
						null, dc.getCompounds(), new AsyncCallback<List<Series>>() {
					public void onSuccess(List<Series> ss) {
						SeriesChartGrid scg = new SeriesChartGrid(chosenDataFilter, ss, true);
						sp.add(scg);
					}
					public void onFailure(Throwable caught) {
						Window.alert("Unable to retrieve data.");
					}
				});	
			}			
			
			Utils.displayInPopup(hp);				
		}
	}
	
	class ToolColumn extends Column<ExpressionRow, String> {
			
		public ToolColumn(ToolCell tc) {
			super(tc);			
		}
		
		public String getValue(ExpressionRow er) { return er.getProbe(); }					
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

	@Override
	public void onResize() {		
		dockPanel.onResize();		
	}
	
}
