package otgviewer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ImageClickCell;
import otgviewer.shared.Association;
import otgviewer.shared.DataColumn;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.Series;
import otgviewer.shared.Synthetic;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;

public class ExpressionTable extends DataListenerWidget {

	private final int PAGE_SIZE = 50;
	
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	private DataGrid<ExpressionRow> exprGrid;
	private DoubleBox absValBox;
	private VerticalPanel seriesChartPanel = new VerticalPanel();	
	private List<SeriesChart> seriesCharts = new ArrayList<SeriesChart>();
	
	private final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	private static otgviewer.client.Resources resources = GWT.create(otgviewer.client.Resources.class);
	
	private List<Synthetic> synthColumns = new ArrayList<Synthetic>();
	
	private ListBox groupsel1 = new ListBox();
	private ListBox groupsel2 = new ListBox();
	
	private Map<String, Association> associations = new HashMap<String, Association>();
	private final static String[] expectedAssociations = new String[] { "KEGG pathways", "MF GO terms", "CC GO terms", "BP GO terms" };	
	private List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
 	private List<AssociationColumn> associationColumns = new ArrayList<AssociationColumn>();
	
	/**
	 * This constructor will be used by the GWT designer. (Not functional at run time)
	 * @wbp.parser.constructor
	 */
	public ExpressionTable() {
		this("400px");
	}	
	
	public ExpressionTable(String height) {
		DockPanel dockPanel = new DockPanel();
		dockPanel.setStyleName("none");
		initWidget(dockPanel);
		dockPanel.setSize("100%", "100%");

		initHideableColumns();
		
		exprGrid = new DataGrid<ExpressionRow>();
		dockPanel.add(exprGrid, DockPanel.CENTER);
		exprGrid.setStyleName("exprGrid");
		exprGrid.setPageSize(PAGE_SIZE);
		exprGrid.setSize("100%", height);
		exprGrid.setSelectionModel(new MultiSelectionModel<ExpressionRow>());
		exprGrid.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {
				for (ExpressionRow r: exprGrid.getDisplayedItems()) {
					if (exprGrid.getSelectionModel().isSelected(r)) {						
						changeProbe(r.getProbe());
					}
				}		
			}
		});
		asyncProvider.addDataDisplay(exprGrid);		
		AsyncHandler colSortHandler = new AsyncHandler(exprGrid);
		
		dockPanel.add(makeToolPanel(), DockPanel.NORTH);
		
		exprGrid.addColumnSortHandler(colSortHandler);

	}
	
	private Widget makeToolPanel() {
		HorizontalPanel tools = new HorizontalPanel();		
		
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		horizontalPanel.setStyleName("colored");
		horizontalPanel.setWidth("");
		tools.add(horizontalPanel);
		tools.setSpacing(5);

		Resources r = GWT.create(Resources.class);
		SimplePager simplePager = new SimplePager(TextLocation.CENTER,
				r, true, 10 * PAGE_SIZE, true);
		simplePager.setStyleName("spacedLayout");
		horizontalPanel.add(simplePager);
		simplePager.setDisplay(exprGrid);
		
		Label label = new Label("Magnitude >=");
		label.setStyleName("highlySpaced");		
		horizontalPanel.add(label);
		label.setWidth("");

		absValBox = new DoubleBox();
		absValBox.setText("0.00");
		horizontalPanel.add(absValBox);

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

		horizontalPanel = new HorizontalPanel();
		tools.add(horizontalPanel);		
		horizontalPanel.setStyleName("colored2");
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		
		horizontalPanel.add(groupsel1);
		groupsel1.setVisibleItemCount(1);
		horizontalPanel.add(groupsel2);
		groupsel2.setVisibleItemCount(1);
		
		
		horizontalPanel.add(new Button("Add T-Test", new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.TTest(null, null), "T-Test"); }							
		}));
		
		horizontalPanel.add(new Button("Add U-Test", new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.UTest(null, null), "U-Test"); }							
		}));
		
		horizontalPanel.add(new Button("Remove tests", new ClickHandler() {
			public void onClick(ClickEvent ce) {
				if (!synthColumns.isEmpty()) {
					synthColumns.clear();
					//We have to reload the data to get rid of the synth columns
					//in our server side session
					getExpressions(chosenProbes);	
				}
			}
		}));
		
		return tools;
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
		
		MenuItem mntmDownloadCsv = new MenuItem("Download CSV", false, new Command() {
			public void execute() {
				kcService.prepareCSVDownload(new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to prepare the requested data for download");
					}
					public void onSuccess(String url) {
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
		r[0] = mntmActions_1;
		
		MenuBar menuBar_2 = new MenuBar(true);

		MenuItem mntmNewMenu_1 = new MenuItem("New menu", false, menuBar_2);

		for (final HideableColumn c: hideableColumns) {
			MenuItem mi = new MenuItem(c.name(), false, new Command() {
				public void execute() {
					c.flipVisibility();					
					setupColumns();
				}
			});
			menuBar_2.addItem(mi);
		}
		
		mntmNewMenu_1.setHTML("Columns");
		r[1] = mntmNewMenu_1;
		return r;
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
				exprGrid.addColumn((Column<ExpressionRow, ?>) c, c.name());				
				extraCols += 1;
			}
		}		


		int i = 0;
		
		for (DataColumn c : chosenColumns) {
			Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, i);
			valueCol.setSortable(true);
			exprGrid.addColumn(valueCol, c.getShortTitle());
			
			if (i == 0 && exprGrid.getColumnSortList().size() == 0) {
				exprGrid.getColumnSortList().push(valueCol);
			}
			i += 1;
		}
		
		for (Synthetic s: synthColumns) {
			Column<ExpressionRow, String> ttestCol = new ExpressionColumn(tc, i);
			ttestCol.setSortable(true);
			exprGrid.addColumn(ttestCol, s.getShortTitle());
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
	
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;

		AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get associations: " + caught.getMessage());
			}
			
			public void onSuccess(Association[] result) {
				associations.clear();
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
					String[] probes = new String[result.size()];
					for (int i = 0; i < probes.length; ++i) {
						probes[i] = result.get(i).getProbe();
					}
					owlimService.associations(chosenDataFilter, probes, assocCallback);
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
	public void columnsChanged(List<DataColumn> columns) {
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
	}
	
	public void beginLoading() {
		exprGrid.setRowCount(0, false);		
	}
	
	void refilterData() { 
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
					}
				});
	}
	
	public void getExpressions(String[] displayedProbes) {
		exprGrid.setRowCount(0, false);
		setupColumns();
		List<DataColumn> cols = new ArrayList<DataColumn>();
		cols.addAll(chosenColumns);

		//set up the series charts
		Set<String> soFar = new HashSet<String>();
		seriesChartPanel.clear();
		seriesCharts.clear();
		
		SeriesChart firstChart = null;
		for (DataColumn c: cols) {
			Label l = new Label(c.getShortTitle());
			seriesChartPanel.add(l);			
			l.setStyleName("heading");
			for (String com: c.getCompounds()) {
				if (!soFar.contains(com)) {
					soFar.add(com);
					SeriesChart sc = new SeriesChart(firstChart != null);
					if (firstChart == null) {
						firstChart = sc;
					} else {
						firstChart.addSlaveChart(sc);
					}					
					seriesChartPanel.add(sc);
					seriesCharts.add(sc);
					this.propagateTo(sc);
					sc.compoundChanged(com);
				}
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
						exprGrid.setRowCount(result);
						exprGrid.setVisibleRangeAndClearData(new Range(0, PAGE_SIZE),
								true);
					}
				});
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
		void flipVisibility();
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
			if (associations.containsKey(assoc)) {
				Association a = associations.get(assoc);
				if (a.data().containsKey(er.getProbe())) {
					return arrayString(a.data().get(er.getProbe()).toArray(new String[0]), ", ");	
				} else {
					return "";
				}
			} else {
				return "";
			}							
		}
		
		public String name() { return assoc; }
		public void flipVisibility() { visible = ! visible; }		
		public boolean visible() { return this.visible; }		
	}
	
	class ToolCell extends ImageClickCell {
		
		public ToolCell(DataListenerWidget owner) {
			super(resources.chart());
		}
		
		//TODO the popup chart code could be cleaned up/factored out quite a bit
		public void onClick(String value) {

			int chartHeight = 200;
			final int numCharts = seriesCharts.size();

			int height = chartHeight * numCharts;			
			for (int i = 0; i < numCharts; i++) {
				SeriesChart seriesChart = (SeriesChart) seriesCharts.get(i);
				seriesChart.probeChanged(value);
				seriesChart.redraw();
				seriesChart.setWidth("500px");
				seriesChart.setPixelHeight(chartHeight);
			}

			HorizontalPanel hp = new HorizontalPanel();
			TabPanel tp = new TabPanel();
			hp.add(tp);
			tp.add(seriesChartPanel, "Individual values");			
			
			VerticalPanel v = new VerticalPanel();			
			tp.add(v, "All time series");			
			tp.selectTab(0);
			
			for (DataColumn dc: chosenColumns) {
				Label l = new Label(dc.getShortTitle());
				l.setStyleName("heading");
				v.add(l);
				final SimplePanel sp = new SimplePanel();				
				v.add(sp);
				kcService.getSeries(chosenDataFilter, new String[] { value }, 
						null, dc.getCompounds(), new AsyncCallback<List<Series>>() {
					public void onSuccess(List<Series> ss) {
						SeriesChartGrid scg = new SeriesChartGrid(ss, true);
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
		public void flipVisibility() { visible = ! visible; }		
	}
	
	@Override
	public void heightChanged(int newHeight) {
		String h3 = (newHeight - exprGrid.getAbsoluteTop() - 15) + "px";
		exprGrid.setHeight(h3);	
	}		
}
