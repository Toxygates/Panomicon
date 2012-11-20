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
import otgviewer.shared.Synthetic;

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
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;

public class ExpressionTable extends DataListenerWidget {
	
	public static interface ExpressionListener {
		public void expressionsChanged(List<ExpressionRow> expressions);		
	}

	// Visible columns
	private boolean geneIdColVis = false, probeColVis = false,
			probeTitleColVis = true, geneSymColVis = true;

	private KCAsyncProvider asyncProvider = new KCAsyncProvider();
	private DataGrid<ExpressionRow> exprGrid;
	private DoubleBox absValBox;
	private VerticalPanel seriesChartPanel = new VerticalPanel();	
	private List<SeriesChart> seriesCharts = new ArrayList<SeriesChart>();

	private final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	
	private List<ExpressionListener> els = new ArrayList<ExpressionListener>();
	private List<Synthetic> synthColumns = new ArrayList<Synthetic>();
	
	private ListBox groupsel1 = new ListBox();
	private ListBox groupsel2 = new ListBox();
	
	private Map<String, Association> associations = new HashMap<String, Association>();
	private final static String[] expectedAssociations = new String[] { "KEGG pathways", "MF GO terms", "CC GO terms", "BP GO terms" };
	private final static Boolean[] assocColumnVis = new Boolean[] { false, false, false, false };
	
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

		exprGrid = new DataGrid<ExpressionRow>();
		dockPanel.add(exprGrid, DockPanel.CENTER);
		exprGrid.setStyleName("exprGrid");
		exprGrid.setPageSize(20);
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
		
		HorizontalPanel tools = new HorizontalPanel();
		dockPanel.add(tools, DockPanel.NORTH);
		
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		horizontalPanel.setStyleName("colored");
		horizontalPanel.setWidth("");
		tools.add(horizontalPanel);
		tools.setSpacing(5);

		Resources r = GWT.create(Resources.class);
		SimplePager simplePager = new SimplePager(TextLocation.CENTER,
				r, true, 100, true);
		simplePager.setStyleName("spacedLayout");
		horizontalPanel.add(simplePager);
		simplePager.setDisplay(exprGrid);
		
		Label label = new Label("Magnitude >=");
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
				refilterData();					
			}
		});

		Button absClear = new Button("No filter");
		horizontalPanel.add(absClear);
		absClear.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent e) {
				absValBox.setValue(0.0);
				refilterData();
			}
		});
		
		horizontalPanel = new HorizontalPanel();
		tools.add(horizontalPanel);		
		horizontalPanel.setStyleName("colored2");
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		
		horizontalPanel.add(groupsel1);
		groupsel1.setVisibleItemCount(1);
		horizontalPanel.add(groupsel2);
		groupsel2.setVisibleItemCount(1);
		
		
		Button b = new Button("Add T-Test");
		horizontalPanel.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.TTest(null, null), "T-Test"); }							
		});
		
		b = new Button("Add U-Test");
		horizontalPanel.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent e) { addTwoGroupSynthetic(new Synthetic.UTest(null, null), "U-Test"); }							
		});
		
		b = new Button("Remove tests");
		horizontalPanel.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				if (!synthColumns.isEmpty()) {
					synthColumns.clear();
					//We have to reload the data completely to get rid of the synth columns
					//in our server side session
					getExpressions(null, true);	
				}
			}
		});
		
		exprGrid.addColumnSortHandler(colSortHandler);

	}
	
	private void addTwoGroupSynthetic(final Synthetic.TwoGroupSynthetic synth, final String name) {
		if (groupsel1.getSelectedIndex() == -1 || groupsel2.getSelectedIndex() == -1) {
			Window.alert("Please select two groups to perform " + name + ".");
		} else if (groupsel1.getSelectedIndex() == groupsel2.getSelectedIndex()) {
			Window.alert("Please select two different groups to perform " + name + ".");
		} else {
			final Group g1 = findGroup(chosenColumns, groupsel1.getItemText(groupsel1.getSelectedIndex()));
			final Group g2 = findGroup(chosenColumns, groupsel2.getItemText(groupsel2.getSelectedIndex()));
			synth.setGroups(g1, g2);
			kcService.addTwoGroupTest(synth, new AsyncCallback<Void>() {
				public void onSuccess(Void v) {
					synthColumns.add(synth);
					setupColumns();
					exprGrid.setVisibleRangeAndClearData(new Range(0, 20), true);
				}
				public void onFailure(Throwable caught) {
					Window.alert("Unable to perform " + name);
				}
			});
		}
	}
	
	private Group findGroup(List<DataColumn> groups, String title) {
		for (DataColumn d: groups) {
			if (((Group) d).getName().equals(title)) {
				return ((Group) d);
			}
		}
		return null;
	}
	
	public void addExpressionListener(ExpressionListener el) {
		els.add(el);
	}
	
	private String downloadUrl;
	private DialogBox db;
	
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
						downloadUrl = url;
						db = new DialogBox(false, true);
						db.setPopupPosition(Window.getClientWidth()/2 - 100, Window.getClientHeight() / 2 - 100);						
						db.setHTML("Your download is ready.");				
						HorizontalPanel hp = new HorizontalPanel();
						
						Button b = new Button("Download");
						b.addClickHandler(new ClickHandler() {
							public void onClick(ClickEvent ev) {
								Window.open(downloadUrl, "_blank", "");
								db.hide();
							}
						});
						hp.add(b);
						b = new Button("Cancel");
						b.addClickHandler(new ClickHandler() {
							public void onClick(ClickEvent ev) {
								db.hide();								
							}
						});
						hp.add(b);
						db.add(hp);
						db.show();						
					}
				});
				
			}
		});
		menuBar_3.addItem(mntmDownloadCsv);		
		r[0] = mntmActions_1;
		
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
		
		for (int i = 0; i < expectedAssociations.length; ++i) {
			MenuItem mi = makeAssocMenuItem(i);
			menuBar_2.addItem(mi);	
		}
		
		mntmNewMenu_1.setHTML("Columns");
		r[1] = mntmNewMenu_1;
		return r;
	}

	private MenuItem makeAssocMenuItem(final int i) {
		MenuItem mi = new MenuItem(expectedAssociations[i], false, new Command() {
			public void execute() {
				assocColumnVis[i] = ! assocColumnVis[i];					
				setupColumns();
			}
		});
		return mi;
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
					return arrayString(er.getGeneIds(), ", ");
				}
			};
			exprGrid.addColumn(geneIdCol, "Gene ID");
			extraCols += 1;
		}

		if (geneSymColVis) {
			TextColumn<ExpressionRow> geneSymCol = new TextColumn<ExpressionRow>() {
				public String getValue(ExpressionRow er) {
					return arrayString(er.getGeneSyms(), ", ");
				}
			};
			exprGrid.addColumn(geneSymCol, "Gene sym");
			extraCols += 1;
		}
		
		for (int i = 0; i < expectedAssociations.length; ++i) {
			if (assocColumnVis[i]) {
				exprGrid.addColumn(new AssociationColumn(tc, expectedAssociations[i]), expectedAssociations[i]);
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
		
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;

		AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get associations");
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
				Window.alert("Unable to get expression values");
			}

			public void onSuccess(List<ExpressionRow> result) {
				if (result.size() > 0) {

					exprGrid.setRowData(start, result);
					for (ExpressionListener el : els) {
						el.expressionsChanged(result);
					}
					
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
	public void probesChanged(String[] probes) {
		//no-op to prohibit change
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
	
	private void refilterData() {
		exprGrid.setRowCount(0, false);
		List<DataColumn> cols = new ArrayList<DataColumn>();
		cols.addAll(chosenColumns);
		kcService.refilterData(chosenDataFilter, cols, 
				absValBox.getValue(), synthColumns, 
				new AsyncCallback<Integer>() {
					public void onFailure(Throwable caught) {						
						Window.alert("Unable to load dataset");					
					}

					public void onSuccess(Integer result) {
						exprGrid.setRowCount(result);
						exprGrid.setVisibleRangeAndClearData(new Range(0, 20),
								true);
					}
				});
	}
	
	public void getExpressions(String[] displayedProbes, boolean usePreviousProbes) {
		if (!usePreviousProbes) {
			changeProbes(displayedProbes);			
		}
		
		exprGrid.setRowCount(0, false);
		setupColumns();
		List<DataColumn> cols = new ArrayList<DataColumn>();
		cols.addAll(chosenColumns);
		
//		List<Barcode> average = new ArrayList<Barcode>();
//		for (DataColumn c: cols) {			
//			average.addAll(Arrays.asList(c.getBarcodes()));
//		}
//		cols.add(new Group("Average", average.toArray(new Barcode[0])));
		
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
						exprGrid.setVisibleRangeAndClearData(new Range(0, 20),
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
	
	class AssociationColumn extends Column<ExpressionRow, String> {
		String assoc;
		TextCell tc;
		
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
	}
	
	class ToolCell extends ImageClickCell {
		
		public ToolCell(DataListenerWidget owner) {
			super("chart_16.png");
		}
		
		public void onClick(String value) {
			PopupPanel pp = new PopupPanel(true, true);

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

//			ScrollPanel sp = new ScrollPanel(seriesChartPanel);			
//			sp.setHeight((Window.getClientHeight() - 100) + "px");			
//			pp.setWidget(sp);					
			seriesChartPanel.setHeight((height + 30 * chosenColumns.size()) + "px");
			Utils.displayInScrolledPopup(seriesChartPanel);
//			pp.setPopupPosition(Window.getClientWidth()/2 - 250, 50);			
//			pp.show();		
		}
	}
	
	class ToolColumn extends Column<ExpressionRow, String> {
			
		public ToolColumn(ToolCell tc) {
			super(tc);			
		}
		
		public String getValue(ExpressionRow er) {
			return er.getProbe();			
		}
	}
	
	@Override
	public void heightChanged(int newHeight) {
		String h3 = (newHeight - exprGrid.getAbsoluteTop() - 45) + "px";
		exprGrid.setHeight(h3);	
	}		
}
