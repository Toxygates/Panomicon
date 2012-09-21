package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.shared.Barcode;
import otgviewer.shared.CellType;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;
import otgviewer.shared.ValueType;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
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
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OTGViewer implements EntryPoint {

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);

	private RootPanel rootPanel;
	private VerticalPanel mainVertPanel;
	private HorizontalSplitPanel horizontalSplitPanel;
	private MenuBar menuBar;

	private DataFilter chosenDataFilter = new DataFilter(CellType.Vivo,
			Organ.Kidney, RepeatType.Single, Organism.Rat);

	
	private ListBox doseLevelList, timeList, barcodeList;
	
	private HorizontalPanel freeSelPanel;
	private TabPanel mainTabPanel;

	private ValueType chosenValueType = ValueType.Folds;
	private ListSelectionHandler<String> compoundHandler, doseHandler,
			timeHandler, groupHandler;
	private MultiSelectionHandler<Barcode> barcodeHandler;

	private TextArea customProbeText;

	// Track the current selection
	// private String[] displayedProbes = null;
	private String chosenCompound;

	private ExpressionTable expressionTable;
	private SeriesChart seriesChart;
	private BioHeatMap bhm;
	private CompoundSelector compoundSelector;
	private GroupInspector groupInspector;
	private ProbeSelector pathwaySel, gotermSel;

	/**
	 * Dummy widget to track listeners
	 */
	private DataListenerWidget listeners = new DataListenerWidget(); 
	
	private Map<String, Screen> screens = new HashMap<String, Screen>();
	
	private MenuBar setupMenu() {

		MenuBar menuBar = new MenuBar(false);
		menuBar.setWidth("100%");
		
		menuBar.addItem(new MenuItem("Open TG-Gates", new Command() {
			public void execute() {}
		}));
		
//		MenuBar menuBar_1 = new MenuBar(true);

//		MenuItem mntmNewMenu = new MenuItem("New menu", false, menuBar_1);
//
//		MenuItem mntmNewItem = new MenuItem("New item", false, new Command() {
//			public void execute() {
//				chosenDataFilter = new DataFilter(CellType.Vitro, Organ.Kidney,
//						RepeatType.Single, Organism.Human);
//				listeners.changeDataFilter(chosenDataFilter);
//				getCompounds();
//			}
//		});
//		mntmNewItem.setHTML("Human, in vitro");
//		menuBar_1.addItem(mntmNewItem);
//
//		MenuItem mntmNewItem_1 = new MenuItem("New item", false, new Command() {
//			public void execute() {
//				chosenDataFilter = new DataFilter(CellType.Vitro, Organ.Kidney,
//						RepeatType.Single, Organism.Rat);
//				listeners.changeDataFilter(chosenDataFilter);
//				getCompounds();
//			}
//		});
//		mntmNewItem_1.setHTML("Rat, in vitro");
//		menuBar_1.addItem(mntmNewItem_1);
//
//		MenuItem mntmNewItem_2 = new MenuItem("New item", false, new Command() {
//			public void execute() {
//				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Liver,
//						RepeatType.Single, Organism.Rat);
//				listeners.changeDataFilter(chosenDataFilter);
//				getCompounds();
//			}
//		});
//		mntmNewItem_2.setHTML("Rat, in vivo, liver, single");
//		menuBar_1.addItem(mntmNewItem_2);
//
//		MenuItem mntmNewItem_3 = new MenuItem("New item", false, new Command() {
//			public void execute() {
//				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Liver,
//						RepeatType.Repeat, Organism.Rat);
//				listeners.changeDataFilter(chosenDataFilter);
//				getCompounds();
//			}
//		});
//		mntmNewItem_3.setHTML("Rat, in vivo, liver, repeat");
//		menuBar_1.addItem(mntmNewItem_3);
//
//		MenuItem mntmNewItem_4 = new MenuItem("New item", false, new Command() {
//			public void execute() {
//				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Kidney,
//						RepeatType.Single, Organism.Rat);
//				listeners.changeDataFilter(chosenDataFilter);
//				getCompounds();
//			}
//		});
//		mntmNewItem_4.setHTML("Rat, in vivo, kidney, single");
//		menuBar_1.addItem(mntmNewItem_4);
//
//		MenuItem mntmNewItem_5 = new MenuItem("New item", false, new Command() {
//			public void execute() {
//				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Kidney,
//						RepeatType.Repeat, Organism.Rat);
//				listeners.changeDataFilter(chosenDataFilter);
//				getCompounds();
//			}
//		});
//		mntmNewItem_5.setHTML("Rat, in vivo, kidney, repeat");
//		menuBar_1.addItem(mntmNewItem_5);
//
//		MenuItemSeparator separator = new MenuItemSeparator();
//		menuBar_1.addSeparator(separator);
//
//		MenuItem mntmFolds = new MenuItem("Fold values", false, new Command() {
//			public void execute() {
//				chosenValueType = ValueType.Folds;
//				listeners.changeValueType(chosenValueType);
//				getExpressions(null, false);
//			}
//		});
//		menuBar_1.addItem(mntmFolds);
//
//		MenuItem mntmAbsoluteValues = new MenuItem(
//				"Absolute expression values", false, new Command() {
//					public void execute() {
//						chosenValueType = ValueType.Absolute;
//						listeners.changeValueType(chosenValueType);
//						getExpressions(null, false);
//					}
//				});
//
//		menuBar_1.addItem(mntmAbsoluteValues);
//		mntmNewMenu.setHTML("Data set");
//		menuBar.addItem(mntmNewMenu);

//		MenuItem mntmSettings = new MenuItem("Settings", false, (Command) null);
//		menuBar.addItem(mntmSettings);

		return menuBar;
	}

	private void resizeInterface(int newHeight) {
		// this is very fiddly and must be tested on all the browsers.
		// Note that simply setting height = 100% won't work.
		String h = (newHeight - rootPanel.getAbsoluteTop() - 20) + "px";
//		String h2 = (newHeight - mainTabPanel.getAbsoluteTop() - 30)
//				+ "px";
//		String h3 = (newHeight - horizontalSplitPanel.getAbsoluteTop() - 40)
//				+ "px";
		
		if (currentScreen != null) {
			currentScreen.resizeInterface(newHeight);
		}
//		mainTabPanel.setHeight(h2);
//		horizontalSplitPanel.setHeight(h3);
//		expressionTable.resizeInterface(newHeight);
		listeners.changeHeight(newHeight);
	}
	
	private Screen currentScreen;

	
	/**
	 * Pick the appropriate screen to display.
	 * @return
	 */
	private Screen pickScreen(String token) {
		if (!screens.containsKey(token)) {
			return screens.get(DatasetScreen.key); //default			
		}
		return screens.get(token);		
	}
	
	private void initScreens() {
		Screen s = new DatasetScreen(null, menuBar);
		screens.put(s.key(), s);
//		s = new CompoundScreen(s, menuBar);
//		screens.put(s.key(), s);
		s = new ColumnScreen(s, menuBar);
		screens.put(s.key(), s);
		s = new ProbeScreen(s, menuBar);
		screens.put(s.key(), s);
		s = new DataScreen(s, menuBar);
		screens.put(s.key(), s);
	}
	
	

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		Runnable onLoadChart = new Runnable() {
			public void run() {

//				 DataTable data = DataTable.create();
//		         data.addColumn(ColumnType.STRING, "Gene Name");
//		         data.addColumn(ColumnType.NUMBER, "chip_XXX_XXX_600");
//		         data.addColumn(ColumnType.NUMBER, "chip2");
//		         data.addColumn(ColumnType.NUMBER, "chip3");
//		         data.addColumn(ColumnType.NUMBER, "chip4");
//		         data.addColumn(ColumnType.NUMBER, "chip5");
//		         data.addColumn(ColumnType.NUMBER, "chip6");
//		         data.addRows(2);         
//		         data.setValue(0, 0, "ATF3");
//		         data.setValue(0, 1, 0);
//		         data.setValue(0, 2, 0.5);
//		         data.setValue(0, 3, 1);
//		         data.setValue(0, 4, 1.5);
//		         data.setValue(0, 5, 2);
//		         data.setValue(0, 6, 2.5);
//		         data.setValue(1, 0, "INS");
//		         data.setValue(1, 1, 3);
//		         data.setValue(1, 2, 3.5);
//		         data.setValue(1, 3, 4);
//		         data.setValue(1, 4, 4.5);
//		         data.setValue(1, 5, 5);
//		         data.setValue(1, 6, 5.5);
//		         
//		         bhm.draw(data);
			}
		};

		VisualizationUtils
				.loadVisualizationApi("1.1", onLoadChart, "corechart");
	
		menuBar = setupMenu();
		initScreens();
		
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			public void onValueChange(ValueChangeEvent<String> vce) {				
				setScreenForToken(vce.getValue());
			}
		});
		
		
		rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.setSize("100%", "100%");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);

		Window.addResizeHandler(new ResizeHandler() {
			public void onResize(ResizeEvent event) {
				resizeInterface(event.getHeight());
			}
		});

		mainVertPanel = new VerticalPanel();
		mainVertPanel.setBorderWidth(0);
		rootPanel.add(mainVertPanel);
		mainVertPanel.setSize("100%", "100%");
		mainVertPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
				
		
		mainVertPanel.add(menuBar);
		
		if ("".equals(History.getToken())) {
			History.newItem(DatasetScreen.key);
		} else {
			setScreenForToken(History.getToken());		
		}
		
	}
	
	private void setScreenForToken(String token) {
		Screen s = pickScreen(token);
		if (currentScreen != null) {
			mainVertPanel.remove(currentScreen);
			currentScreen.hide();
		}
		currentScreen = s;
		currentScreen.show();					
		mainVertPanel.add(currentScreen);
		resizeInterface(Window.getClientHeight()); 
	}
	
	public void onModuleLoadRest() {
		mainTabPanel = new TabPanel();
//		mainVertPanel.add(mainTabPanel); //!!		
		mainTabPanel.setSize("100%", "100%");
		mainTabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
			public void onSelection(SelectionEvent<Integer> event) {
				switch (event.getSelectedItem()) {
				case 0:
					// group definition tab
					expressionTable.deactivate();
					seriesChart.deactivate();
					break;
				case 1:
					// data viewer tab
					expressionTable.activate();
					seriesChart.deactivate();
					groupHandler.setItems(groupInspector.getGroups().keySet());
					
					//the horizontal split panel has a bug that makes it work unreliably inside tab panels.
					//this works around the bug.
					horizontalSplitPanel.setSplitPosition("200px");
					break;
				case 2:
					expressionTable.deactivate();
					seriesChart.activate();

					// series chart tab
					seriesChart.redraw();
				}
			}
		});

		DockPanel groupDefDock = new DockPanel();
		mainTabPanel.add(groupDefDock, "Group definitions", false);
		groupDefDock.setSize("5cm", "3cm");

		compoundSelector = new CompoundSelector("1. Compounds");
		compoundSelector.changeDataFilter(chosenDataFilter);
		groupDefDock.add(compoundSelector, DockPanel.WEST);

		groupInspector = new GroupInspector(compoundSelector);
		groupDefDock.add(groupInspector, DockPanel.CENTER);
		compoundSelector.addListener(groupInspector);
		
		horizontalSplitPanel = new HorizontalSplitPanel();
		horizontalSplitPanel.setStyleName("spacedLayout");		
		horizontalSplitPanel.setSize("100%", "800px");

		mainTabPanel.add(horizontalSplitPanel, "Data viewer", false);
		
		VerticalPanel probeSelVert = new VerticalPanel();
		DockPanel dataViewDock = new DockPanel();			
		dataViewDock.setSize("100%", "100%");
		
		horizontalSplitPanel.setLeftWidget(probeSelVert);
		horizontalSplitPanel.setRightWidget(dataViewDock);
		horizontalSplitPanel.setSplitPosition("200px");
		
		TabPanel innerTabPanel = new TabPanel();
		dataViewDock.add(innerTabPanel, DockPanel.NORTH);
		innerTabPanel.setSize("100%", "281px");

		freeSelPanel = new HorizontalPanel();
		innerTabPanel.add(freeSelPanel, "Free selection", false);
		freeSelPanel.setSize("5cm", "3cm");
		freeSelPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		HorizontalPanel horizontalPanel_1 = new HorizontalPanel();
		innerTabPanel.add(horizontalPanel_1, "Groups", false);
		horizontalPanel_1.setSize("5cm", "236px");

		ListBox groupList = new ListBox();
		horizontalPanel_1.add(groupList);
		groupList.setMultipleSelect(true);
		groupList.setSize("200px", "236px");
		groupList.setVisibleItemCount(5);
		groupHandler = new MultiSelectionHandler<String>("groups", groupList) {
			protected void getUpdates(List<String> groups) {
				List<DataColumn> cols = new ArrayList<DataColumn>();
				for (String s : groups) {
					cols.add(groupInspector.getGroups().get(s));
				}
				listeners.columnsChanged(cols);
				getExpressions(null, true);
			}
		};

		VerticalPanel verticalPanel_1 = new VerticalPanel();
		freeSelPanel.add(verticalPanel_1);

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
				listeners.changeCompound(compound);
				getDoseLevels(compound, chosenDataFilter.organ.toString());
				getTimes(compound, chosenDataFilter.organ.toString());
			}
		};

		VerticalPanel verticalPanel_4 = new VerticalPanel();
		freeSelPanel.add(verticalPanel_4);

		Label label_1 = new Label("Doses");
		verticalPanel_4.add(label_1);

		doseLevelList = new ListBox();
		doseLevelList.setVisibleItemCount(10);
		verticalPanel_4.add(doseLevelList);
		doseLevelList.setSize("10em", "100px");

		doseHandler = new ListSelectionHandler<String>("dose levels",
				doseLevelList, true, SeriesDisplayStrategy.VsDose.allDoses) {
			protected void getUpdates(String dose) {
				// updateSelections();
				getBarcodes(compoundHandler.lastSelected(),
						chosenDataFilter.organ.toString(),
						doseHandler.lastSelected(), timeHandler.lastSelected());

			}
		};

		Label label_2 = new Label("Times");
		verticalPanel_4.add(label_2);

		timeList = new ListBox();
		timeList.setVisibleItemCount(5);
		verticalPanel_4.add(timeList);
		timeList.setSize("10em", "100px");

		timeHandler = new ListSelectionHandler<String>("times", timeList, true,
				SeriesDisplayStrategy.VsTime.allTimes) {
			protected void getUpdates(String time) {
				// updateSelections();
				getBarcodes(compoundHandler.lastSelected(),
						chosenDataFilter.organ.toString(),
						doseHandler.lastSelected(), timeHandler.lastSelected());
			}
		};

		VerticalPanel verticalPanel_5 = new VerticalPanel();
		freeSelPanel.add(verticalPanel_5);

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
				// !!!
				listeners.columnsChanged(Arrays.asList(barcodes
						.toArray(new DataColumn[0])));
				getExpressions(null, true);
			}

			protected String representation(DataColumn b) {
				return b.getShortTitle();
			}
		};
		
		bhm = new BioHeatMap(BioHeatMap.Options.create());
		freeSelPanel.add(bhm);

		expressionTable = new ExpressionTable("400px");
		MenuItem[] mis = expressionTable.menuItems();
		menuBar.addItem(mis[0]);
		menuBar.addItem(mis[1]);
		
		dataViewDock.add(expressionTable, DockPanel.CENTER);
		expressionTable.setWidth("100%");

		compoundHandler.addAfter(doseHandler);
		compoundHandler.addAfter(timeHandler);
		doseHandler.addAfter(barcodeHandler);
		timeHandler.addAfter(barcodeHandler);

		seriesChart = new SeriesChart();
		mainTabPanel.add(seriesChart, "Probe chart", false);
		
		probeSelVert.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);		
		probeSelVert.setSize("100%", "100%");
		probeSelVert.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		
		Label l = new Label("Probe selection");
		l.setStyleName("heading");
		probeSelVert.add(l);
		
		StackPanel probeSelStack = new StackPanel();
		probeSelStack.setStyleName("gwt-StackPanel");
		probeSelVert.add(probeSelStack);
		probeSelStack.setSize("", "592px");
		
		pathwaySel = new ProbeSelector("This lets you view probes that correspond to a given KEGG pathway. " +
				"Enter a partial pathway name and press enter to search.", false) {
			protected void getMatches(String pattern) {
					owlimService.pathways(chosenDataFilter, pattern,
						retrieveMatchesCallback());
			}
			protected void getProbes(String item) {
				expressionTable.beginLoading();
				owlimService.probesForPathway(chosenDataFilter, item,
						retrieveProbesCallback());
			}
			public void probesChanged(String[] probes) {
				super.probesChanged(probes);
				getExpressions(probes, false);
			}
		};
		probeSelStack.add(pathwaySel, "KEGG pathway search", false);
		pathwaySel.setSize("100%", "350px");
		
		gotermSel = new ProbeSelector("This lets you view probes that correspond to a given GO term. " +
 "Enter a partial term name and press enter to search.", false) {
			protected void getMatches(String pattern) {
				owlimService.goTerms(pattern, retrieveMatchesCallback());
			}

			protected void getProbes(String item) {
				expressionTable.beginLoading();
				owlimService.probesForGoTerm(chosenDataFilter, item, retrieveProbesCallback());
			}

			public void probesChanged(String[] probes) {
				super.probesChanged(probes);
				getExpressions(probes, false);
			}
		};
		probeSelStack.add(gotermSel, "GO term search", false);
		gotermSel.setSize("100%", "350px");
		
		
		Widget chembl = makeTargetLookupPanel("CHEMBL", 
				"This lets you view probes that are known targets of the currently selected compound.", 
				"Show CHEMBL targets");
		probeSelStack.add(chembl, "CHEMBL targets", false);
				
		Widget drugBank = makeTargetLookupPanel("DrugBank", 
				"This lets you view probes that are known targets of the currently selected compound.", 
				"Show DrugBank targets");
		probeSelStack.add(drugBank, "DrugBank targets", false);
	
		
		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		probeSelStack.add(verticalPanel_3, "Custom", false);
		verticalPanel_3.setSize("100%", "350px");
		
		Label label_5 = new Label("Enter a list of probes, genes or proteins to display only those.");
		label_5.setStyleName("none");
		verticalPanel_3.add(label_5);
		
		customProbeText = new TextArea();
		verticalPanel_3.add(customProbeText);
		customProbeText.setSize("95%", "300px");
		
		Button button_1 = new Button("Show custom probes");
		verticalPanel_3.add(button_1);
		button_1.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String text = customProbeText.getText();
				String[] split = text.split("\n");
				if (split.length == 0) {
					Window.alert("Please enter probes, genes or proteins in the text box and try again.");
				} else {
					// change the identifiers (which can be mixed format) into a
					// homogenous format (probes only)
					kcService.identifiersToProbes(chosenDataFilter, split,
							new AsyncCallback<String[]>() {
								public void onSuccess(String[] probes) {
									getExpressions(probes, false);
								}

								public void onFailure(Throwable caught) {

								}
							});
				}
			}
		});
		
		Button button_2 = new Button("Show all probes");
		probeSelVert.add(button_2);
		button_2.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				// if (pathwayList.getSelectedIndex() != -1) {
				// pathwayList.setItemSelected(pathwayList.getSelectedIndex(),
				// false);
				// }
				getExpressions(null, false);
			}
		});

		// wiring
		listeners.addListener(expressionTable);
		listeners.addListener(seriesChart);
		listeners.addListener(compoundSelector);
		expressionTable.addListener(seriesChart);
		listeners.addListener(pathwaySel);
		listeners.addListener(gotermSel);
		
		// initial settings
		listeners.changeDataFilter(chosenDataFilter);
		listeners.changeValueType(chosenValueType);

		// now when all widgets are in place we can do this (will trigger some
		// activity)
		mainTabPanel.selectTab(0);
		innerTabPanel.selectTab(0);

		// everything has been set up, set the initial size
		resizeInterface(Window.getClientHeight());
		// INITIAL DATA
		getCompounds();
	}

	private Widget makeTargetLookupPanel(final String service, String label, String buttonText) {
		VerticalPanel verticalPanel_2 = new VerticalPanel();
		verticalPanel_2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);		
		verticalPanel_2.setSize("100%", "100px");		
		
		Label label_4 = new Label(label);
		verticalPanel_2.add(label_4);
		
		Button button = new Button(buttonText);
		verticalPanel_2.add(button);
		button.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (chosenCompound != null) {
					expressionTable.beginLoading();
					owlimService.probesTargetedByCompound(chosenDataFilter,
							chosenCompound, service, new AsyncCallback<String[]>() {
								public void onFailure(Throwable caught) {
									Window.alert("Unable to get probes.");
								}

								public void onSuccess(String[] probes) {
									getExpressions(probes, false);
								}
							});
				} else {
					Window.alert("Please select a compound first.");
				}
			}
		});
		return verticalPanel_2;
	}
	
	/**
	 * This method is called when selection variables have changed and this
	 * needs to be reflected.
	 */
	// void updateSelections() {
	// expressionTable.setDataFilter(chosenDataFilter);
	// expressionTable.setValueType(chosenValueType);
	//
	// seriesChart.setCompound(chosenCompound);
	// seriesChart.setDataFilter(chosenDataFilter);
	// seriesChart.setValueType(chosenValueType);
	//
	// compoundSelector.dataFilterChanged(chosenDataFilter);
	// }

	void getCompounds() {
		compoundHandler.clearForLoad();
		owlimService.compounds(chosenDataFilter,
				compoundHandler.retrieveCallback());
	}

	void getDoseLevels(String compound, String organ) {
		doseHandler.clearForLoad();		
		owlimService.doseLevels(chosenDataFilter, compound, organ,
				doseHandler.retrieveCallback());
	}

	void getBarcodes(String compound, String organ, String doseLevel,
			String time) {
		barcodeHandler.clearForLoad();		
		owlimService.barcodes(chosenDataFilter, compound, organ, doseLevel,
				time, barcodeHandler.retrieveCallback());
	}

	void getTimes(String compound, String organ) {
		timeHandler.clearForLoad();
		owlimService.times(chosenDataFilter, compound, organ,
				timeHandler.retrieveCallback());
	}

	void getExpressions(String[] probes, boolean usePreviousProbes) {
		expressionTable.getExpressions(probes, usePreviousProbes);
	}
}
