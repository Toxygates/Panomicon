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
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;

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
	
	private DataFilter chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Kidney, 
			RepeatType.Single, Organism.Rat);
	
	private ListBox pathwayList, doseLevelList, timeList,
			barcodeList;
	private HorizontalPanel horizontalPanel;
	

	private ValueType chosenValueType = ValueType.Folds;
	private ListSelectionHandler<String> compoundHandler, doseHandler,
			timeHandler, pathwayHandler;
	private MultiSelectionHandler<Barcode> barcodeHandler;

	private TextBox pathwayBox;
	private TextArea customProbeText;

	// Track the current selection
	private String[] displayedProbes = null; 	
	private String chosenCompound;
	
	private ExpressionTable expressionTable;
	private SeriesChart seriesChart;


	private MenuBar setupMenu() {
	
		MenuBar menuBar = new MenuBar(false);
		menuBar.setWidth("100%");
		MenuBar menuBar_1 = new MenuBar(true);

		MenuItem mntmNewMenu = new MenuItem("New menu", false, menuBar_1);

		MenuItem mntmNewItem = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenDataFilter = new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Human);
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem.setHTML("Human, in vitro");
		menuBar_1.addItem(mntmNewItem);

		MenuItem mntmNewItem_1 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenDataFilter = new DataFilter(CellType.Vitro, Organ.Kidney, RepeatType.Single, Organism.Rat);
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_1.setHTML("Rat, in vitro");
		menuBar_1.addItem(mntmNewItem_1);

		MenuItem mntmNewItem_2 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Single, Organism.Rat);
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_2.setHTML("Rat, in vivo, liver, single");
		menuBar_1.addItem(mntmNewItem_2);

		MenuItem mntmNewItem_3 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Liver, RepeatType.Repeat, Organism.Rat);
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_3.setHTML("Rat, in vivo, liver, repeat");
		menuBar_1.addItem(mntmNewItem_3);

		MenuItem mntmNewItem_4 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Single, Organism.Rat);
				updateSelections();
				getCompounds();
			}
		});
		mntmNewItem_4.setHTML("Rat, in vivo, kidney, single");
		menuBar_1.addItem(mntmNewItem_4);

		MenuItem mntmNewItem_5 = new MenuItem("New item", false, new Command() {
			public void execute() {
				chosenDataFilter = new DataFilter(CellType.Vivo, Organ.Kidney, RepeatType.Repeat, Organism.Rat);
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
				getExpressions(null, false);
			}
		});
		menuBar_1.addItem(mntmFolds);

		MenuItem mntmAbsoluteValues = new MenuItem("Absolute expression values", false,
				new Command() {
					public void execute() {
						chosenValueType = ValueType.Absolute;
						updateSelections();
						getExpressions(null, false);
					}
				});

		menuBar_1.addItem(mntmAbsoluteValues);
		mntmNewMenu.setHTML("Data set");
		menuBar.addItem(mntmNewMenu);

		MenuItem mntmSettings = new MenuItem("Settings", false, (Command) null);
		menuBar.addItem(mntmSettings);
		
		return menuBar;
	}
	
	private void resizeInterface(int newHeight) {
		//this is very fiddly and must be tested on all the browsers.
		//Note that simply setting height = 100% won't work.
		String h = (newHeight - rootPanel.getAbsoluteTop() - 20) + "px";
		String h2 = (newHeight - horizontalSplitPanel.getAbsoluteTop() - 30) + "px";
		expressionTable.resizeInterface(newHeight);
	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		Runnable onLoadChart = new Runnable() {
			public void run() {
				seriesChart.onLoadChart();
								
			}
		};

		VisualizationUtils.loadVisualizationApi("1.1", onLoadChart, "corechart");

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
		mainVertPanel.setSize("100%", "800px");

		menuBar = setupMenu();
		mainVertPanel.add(menuBar);

		horizontalSplitPanel = new HorizontalSplitPanel();
		horizontalSplitPanel.setStyleName("spacedLayout");
		horizontalSplitPanel.setSplitPosition("200px");
		mainVertPanel.add(horizontalSplitPanel);
		horizontalSplitPanel.setSize("100%", "800px");

		//PATHWAY SEARCH
		VerticalPanel verticalPanel_2 = new VerticalPanel();
		verticalPanel_2.setStyleName("spacedLayout");
		verticalPanel_2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		horizontalSplitPanel.setLeftWidget(verticalPanel_2);
		verticalPanel_2.setBorderWidth(0);
		verticalPanel_2.setSize("95%", "95%");

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
		pathwayList.setSize("100%", "309px");
		pathwayList.setVisibleItemCount(5);

		pathwayHandler = new ListSelectionHandler<String>("pathways",
				pathwayList, false) {
			protected void getUpdates(String pathway) {
				owlimService.probesForPathway(chosenDataFilter, pathway, new AsyncCallback<String[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to get probes.");
					}

					public void onSuccess(String[] probes) {						
						getExpressions(probes, false);
					}
				});
			}
		};
		
		Button btnShowCompoundTargets = new Button("Show CHEMBL targets");
		verticalPanel_2.add(btnShowCompoundTargets);
		btnShowCompoundTargets.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (chosenCompound != null) {
					owlimService.probesTargetedByCompound(chosenDataFilter, chosenCompound, 
							new AsyncCallback<String[]>() {
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
		
		Label lblEnterProbesManually = new Label("Custom probe list");
		verticalPanel_2.add(lblEnterProbesManually);
		
		customProbeText = new TextArea();
		verticalPanel_2.add(customProbeText);
		customProbeText.setSize("95%", "100px");
		
		Button btnShowCustomProbes = new Button("Show custom probes");
		verticalPanel_2.add(btnShowCustomProbes);
		btnShowCustomProbes.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String text = customProbeText.getText();
				String[] split = text.split("\n");
				if (split.length == 0) {
					Window.alert("Please enter probes, genes or proteins in the text box and try again.");					
				} else {
					//change the identifiers (which can be mixed format) into a homogenous format (probes only)
					//todo: might want to display some kind of progress indicator
					kcService.identifiersToProbes(chosenDataFilter, split, new AsyncCallback<String[]>() {
						public void onSuccess(String[] probes) {							
							getExpressions(probes, false);
						}
						public void onFailure(Throwable caught) {
							
						}
					});
				}
			}
		});

		Button btnShowAllProbes = new Button("Show all probes");
		verticalPanel_2.add(btnShowAllProbes);
		btnShowAllProbes.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				displayedProbes = null;
				if (pathwayList.getSelectedIndex() != -1) {
					pathwayList.setItemSelected(pathwayList.getSelectedIndex(),
							false);
				}
				getExpressions(null, false);
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
					updateSelections(); //get the selected probe
					seriesChart.redraw();					
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
				getDoseLevels(compound, chosenDataFilter.organ.toString());
				getTimes(compound, chosenDataFilter.organ.toString());
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
				doseLevelList, true, SeriesDisplayStrategy.VsDose.allDoses) {
			protected void getUpdates(String dose) {
				updateSelections();
				getBarcodes(compoundHandler.lastSelected(),
						chosenDataFilter.organ.toString(), doseHandler.lastSelected(),
						timeHandler.lastSelected());

			}
		};

		Label label_2 = new Label("Times");
		verticalPanel_4.add(label_2);

		timeList = new ListBox();
		timeList.setVisibleItemCount(5);
		verticalPanel_4.add(timeList);
		timeList.setSize("10em", "100px");

		timeHandler = new ListSelectionHandler<String>("times", timeList, true, SeriesDisplayStrategy.VsTime.allTimes) {
			protected void getUpdates(String time) {
				updateSelections();
				getBarcodes(compoundHandler.lastSelected(),
						chosenDataFilter.organ.toString(), doseHandler.lastSelected(),
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
				expressionTable.setSelectedBarcodes(barcodes);
				getExpressions(null, true);
			}

			protected String representation(Barcode b) {
				return b.getTitle();
			}
		};

		
		expressionTable = new ExpressionTable(menuBar);
		dockPanel.add(expressionTable, DockPanel.CENTER);
		
		compoundHandler.addAfter(doseHandler);
		compoundHandler.addAfter(timeHandler);
		doseHandler.addAfter(barcodeHandler);
		timeHandler.addAfter(barcodeHandler);

		seriesChart = new SeriesChart();
		tabPanel.add(seriesChart, "Chart", false);
		seriesChart.setDataFilter(chosenDataFilter);		

		//everything has been set up, set the initial size
		resizeInterface(Window.getClientHeight());
		//INITIAL DATA
		getCompounds();
	}
	

	/**
	 * This method is called when selection variables have changed
	 * and this needs to be reflected.
	 */
	void updateSelections() {		
		expressionTable.setDataFilter(chosenDataFilter);
		expressionTable.setValueType(chosenValueType);
		
		seriesChart.setCompound(chosenCompound);
		seriesChart.setDataFilter(chosenDataFilter);
		seriesChart.setValueType(chosenValueType);
		seriesChart.setProbe(expressionTable.getChosenProbe());
	}

	void getCompounds() {
		owlimService.compounds(chosenDataFilter, compoundHandler.retrieveCallback());
	}

	void getDoseLevels(String compound, String organ) {
		doseLevelList.clear();
		owlimService.doseLevels(chosenDataFilter, compound, organ, 
				doseHandler.retrieveCallback());
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


	void getPathways(String pattern) {
		owlimService.pathways(chosenDataFilter, pattern, pathwayHandler.retrieveCallback());
	}

	
	void getExpressions(String[] probes, boolean usePreviousProbes) {
		expressionTable.getExpressions(probes, usePreviousProbes);
	}
	
}
