/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.FixedWidthLayoutPanel;
import otgviewer.client.components.ListChooser;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.shared.Group;
import t.common.client.components.ResizingDockLayoutPanel;
import t.common.client.components.ResizingListBox;
import t.common.shared.ItemList;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.viewer.client.Utils;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The probe selection screen.
 */
public class ProbeScreen extends Screen {

	public static final String key = "probes";
	private final SparqlServiceAsync sparqlService;			
	private final MatrixServiceAsync matrixService;

	private TextArea customProbeText;
	private ListBox probesList;
	private Set<String> listedProbes = new HashSet<String>();
	private List<ListBox> compoundLists = new ArrayList<ListBox>();	
	private Button proceedSelected;
	private FixedWidthLayoutPanel fwlp;
	private DockLayoutPanel plPanel;
	private Widget plNorth, plSouth;
	private ListChooser listChooser;
	private static final int PL_NORTH_HEIGHT = 30;
	private static final int PL_SOUTH_HEIGHT = 30;

	private static final int STACK_ITEM_HEIGHT = 29;
	private final GeneOracle oracle;
	private final Logger logger = SharedUtils.getLogger();

	public ProbeScreen(ScreenManager man) {
		super("Probe selection", key, true, man, resources
				.probeSelectionHTML(), resources.probeSelectionHelp());
		oracle = new GeneOracle(this);
		sparqlService = man.sparqlService();
		matrixService = man.matrixService();
	}

	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key);
	}
	
	private ProbeSelector pathwaySelector() {
		return new ProbeSelector(
				"This lets you view probes that correspond to a given KEGG pathway. "
						+ "Enter a partial pathway name and press enter to search.",
				true) {
			protected void getMatches(String pattern) {
				sparqlService.pathways(chosenSampleClass, pattern,
						retrieveMatchesCallback());
			}

			protected void getProbes(String item) {
				sparqlService.probesForPathway(chosenSampleClass, item, getAllSamples(),
						retrieveProbesCallback());
			}

			@Override
			public void probesChanged(String[] probes) {
				super.probesChanged(probes);
				addProbes(probes);
			}
		};
	}

	private ProbeSelector goTermSelector() {
		return new ProbeSelector(
				"This lets you view probes that correspond to a given GO term. "
						+ "Enter a partial term name and press enter to search.",
				true) {
			protected void getMatches(String pattern) {
				sparqlService.goTerms(pattern, retrieveMatchesCallback());
			}

			protected void getProbes(String item) {
				sparqlService.probesForGoTerm(item, getAllSamples(),
						retrieveProbesCallback());
			}

			@Override
			public void probesChanged(String[] probes) {
				super.probesChanged(probes);
				addProbes(probes);
			}
		};
	}
	
	private VerticalPanel innerVP(String l) {
		VerticalPanel vpii = Utils.mkVerticalPanel();
		vpii.setWidth("100%");		
		vpii.setStylePrimaryName("colored-margin");		
		
		Label label = new Label(l);
		vpii.add(label);
		return vpii;
	}
	
	protected String freeLookupMessage() {
		return "Enter a list of probes, genes or proteins to display only those.";
	}

	private Widget manualSelection() {
		VerticalPanel vp = new VerticalPanel();
		vp.setSize("100%", "100%");
		vp.setSpacing(5);
		vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		VerticalPanel vpi = Utils.mkVerticalPanel();
		vpi.setWidth("100%");
		vp.add(vpi);

		VerticalPanel vpii = innerVP(freeLookupMessage());		
		vpi.add(vpii);

		customProbeText = new TextArea();
		vpii.add(customProbeText);
		customProbeText.setVisibleLines(10);
		customProbeText.setWidth("95%");

		vpii.add(new Button("Add manual list", new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String text = customProbeText.getText();
				String[] split = text.split("[\n ,\t]");

				if (split.length == 0) {
					Window.alert("Please enter identifiers in the text box and try again.");
				} else {
					addManualProbes(split, false);
				}
			}
		}));

		if (hasSymbolFinder()) {
			vpii = innerVP("Begin typing a gene symbol to get suggestions.");
			vpi.add(vpii);

			final SuggestBox sb = new SuggestBox(oracle);
			vpii.add(sb);
			sb.setWidth("95%");
			vpii.add(new Button("Add gene", new ClickHandler() {
				public void onClick(ClickEvent ev) {
					String[] gs = new String[1];
					if (sb.getText().length() == 0) {
						Window.alert("Please enter a gene symbol and try again.");
					}
					gs[0] = sb.getText();
					addManualProbes(gs, false);
				}
			}));
		}
		
		if (hasPartialMatcher()) {
			vpii = innerVP("Match by partial probe name:");
			vpi.add(vpii);

			//TODO "filter" function as well as "add"
			final TextBox tb = new TextBox();
			vpii.add(tb);
			tb.setWidth("95%");
			vpii.add(new Button("Add", new ClickHandler() {
				public void onClick(ClickEvent ev) {
					String[] gs = new String[1];
					if (tb.getText().length() == 0) {
						Window.alert("Please enter a pattern and try again.");
					}
					gs[0] = tb.getText();
					addManualProbes(gs, true);
				}
			}));
		}
		
		return vp;
	}
	
	protected boolean hasChembl() { return true; }
	
	protected boolean hasDrugbank() { return true; }
	
	protected boolean hasSymbolFinder() { return true; }
	
	protected boolean hasPartialMatcher() { return false; }

	public Widget content() {
		StackLayoutPanel probeSelStack = new StackLayoutPanel(Unit.PX);
		probeSelStack.setWidth("350px");
		
		ProbeSelector psel = pathwaySelector();
		probeSelStack.add(psel, "KEGG pathway search", STACK_ITEM_HEIGHT);
		addListener(psel);
		psel = goTermSelector();
		probeSelStack.add(psel, "GO term search", STACK_ITEM_HEIGHT);
		addListener(psel);

		if (hasChembl()) {
			Widget chembl = makeTargetLookupPanel(
					"CHEMBL",
					"This lets you view probes that are known targets of the currently selected compound.");
			probeSelStack.add(chembl, "CHEMBL targets", STACK_ITEM_HEIGHT);
		}

		if (hasDrugbank()) {
			Widget drugBank = makeTargetLookupPanel(
					"DrugBank",
					"This lets you view probes that are known targets of the currently selected compound.");
			probeSelStack.add(drugBank, "DrugBank targets", STACK_ITEM_HEIGHT);
		}

		probeSelStack.add(manualSelection(), "Free selection",
				STACK_ITEM_HEIGHT);

		Label l = new Label("Selected probes");
		l.setStylePrimaryName("heading");

		probesList = new ResizingListBox(74);
		probesList.setWidth("100%");

	    HorizontalPanel buttons = Utils.mkHorizontalPanel(false);
        Button b = new Button("Clear selected probes", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				probesChanged(new String[0]);
			}
		});
        
        final ProbeScreen ps = this;	    
	    buttons.add(b);

		plPanel = new ResizingDockLayoutPanel();
		plNorth = Utils.wideCentered(l);
		plSouth = Utils.wideCentered(buttons);

		plPanel.addNorth(plNorth, PL_NORTH_HEIGHT);
		plPanel.addSouth(plSouth, PL_SOUTH_HEIGHT);
		
		listChooser = new ListChooser(appInfo().predefinedProbeLists(), "probes") {
			@Override
			protected void itemsChanged(List<String> items) {
				matrixService.identifiersToProbes(items.toArray(new String[0]),
						true, false, getAllSamples(), new PendingAsyncCallback<String[]>(ps) {
							@Override
							public void handleSuccess(String[] t) {
								ps.probesChanged(t);								
							}							
						});
			}				
			
			@Override
			protected void listsChanged(List<ItemList> lists) {
				ps.chosenItemLists = lists;
				ps.storeItemLists(ps.getParser(ps));
			}
		};
		listChooser.setStylePrimaryName("colored");
		//To ensure that ListChooser has chosenColumns
		addListener(listChooser);
		
		plPanel.addNorth(listChooser, PL_NORTH_HEIGHT);
		
		plPanel.add(probesList);

		DockLayoutPanel dp = new ResizingDockLayoutPanel();
		dp.addWest(probeSelStack, 100);
		dp.add(plPanel);

		fwlp = new FixedWidthLayoutPanel(dp, 700, 10);
		fwlp.setSize("100%", "100%");

		return fwlp;
	}

	@Override
	public Widget bottomContent() {
		HorizontalPanel buttons = Utils.mkHorizontalPanel(false);
		final Screen sc = this;
		proceedSelected = new Button("Proceed with selected probes",
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						if (listedProbes.size() == 0) {
							Window.alert("Please select the probes you are interested in, or proceed with all probes.");
						} else {
							probesChanged(listedProbes.toArray(new String[0]));						
							StorageParser p = getParser(sc);
							storeProbes(p);
							logger.info("Saved list of " + chosenProbes.length + " probes");
							configuredProceed(DataScreen.key);
						}
					}
				});
		buttons.add(proceedSelected);
		updateProceedButton();

		buttons.add(new Button("Proceed with all probes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (listedProbes.size() == 0
						|| Window.confirm("Proceeding will erase your list of "
								+ listedProbes.size() + " selected probes.")) {
					probesChanged(new String[0]);
					StorageParser p = getParser(sc);
					storeProbes(p);
					logger.info("Saved empty probe list");
					configuredProceed(DataScreen.key);
				} else {
					logger.info("User refused to proceed.");
				}
			}
		}));
		return buttons;
	}

	private void addManualProbes(String[] probes, boolean titleMatch) {
		// change the identifiers (which can be mixed format, for example genes
		// and proteins etc) into a
		// homogenous format (probes only)
		matrixService.identifiersToProbes(probes, true, titleMatch, getAllSamples(),
				new PendingAsyncCallback<String[]>(this,
						"Unable to obtain manual probes (technical error).") {
					public void handleSuccess(String[] probes) {
						if (probes.length == 0) {
							Window.alert("No matching probes were found.");
						} else {
							addProbes(probes);
						}
					}
				});
	}

	private ClickHandler makeTargetLookupCH(final ListBox compoundList,
			final String service, final boolean homologs) {
		final DataListenerWidget w = this;
		return new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (compoundList.getSelectedIndex() != -1) {
					String compound = compoundList.getItemText(compoundList
							.getSelectedIndex());
					sparqlService.probesTargetedByCompound(chosenSampleClass,
							compound, service, homologs,
							new PendingAsyncCallback<String[]>(w,
									"Unable to get probes (technical error).") {
								public void handleSuccess(String[] probes) {
									if (probes.length == 0) {
										Window.alert("No matching probes were found.");
									} else {
										addProbes(probes);
									}
								}
							});
				} else {
					Window.alert("Please select a compound first.");
				}
			}
		};
	}

	private Widget makeTargetLookupPanel(final String service, String label) {
		VerticalPanel vp = new VerticalPanel();
		vp.setSize("100%", "100%");
		vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

		VerticalPanel vpi = Utils.mkVerticalPanel(true);
		vpi.setStylePrimaryName("colored");
		Label l = new Label(label);
		vpi.add(l);

		final ListBox compoundList = new ListBox();
		compoundLists.add(compoundList);
		vpi.add(compoundList);

		Button button = new Button("Add direct targets >>", makeTargetLookupCH(
				compoundList, service, false));
		vpi.add(button);

		button = new Button("Add inferred targets >>", makeTargetLookupCH(
				compoundList, service, true));
		vpi.add(button);

		vp.add(vpi);
		return vp;
	}

	/**
	 * Obtain the gene symbols of the requested probes, then add them and
	 * display them. Probes must be unique.
	 * 
	 * @param probes
	 */
	private void addProbes(String[] probes) {
		for (String p : probes) {
			listedProbes.add(p);
		}
		listChooser.setItems(new ArrayList<String>(listedProbes));
		
		final String[] probesInOrder = listedProbes.toArray(new String[0]);
		chosenProbes = probesInOrder;
		StorageParser p = getParser(this);
		storeProbes(p);

		if (probes.length > 0) {
			// TODO reduce the number of ajax calls done by this screen by
			// collapsing them
			sparqlService.geneSyms(probesInOrder,
					new AsyncCallback<String[][]>() {
						public void onSuccess(String[][] syms) {
							deferredAddProbes(probesInOrder, syms);
						}

						public void onFailure(Throwable caught) {
							Window.alert("Unable to get gene symbols for probes.");
						}
					});
		}
		updateProceedButton();
	}

	/**
	 * Display probes with gene symbols. Probes must be unique.
	 * 
	 * @param probes
	 * @param syms
	 */
	private void deferredAddProbes(String[] probes, String[][] syms) {
		probesList.clear();
		for (int i = 0; i < probes.length; ++i) {
			if (syms[i].length > 0) {
				probesList.addItem(SharedUtils.mkString(syms[i], "/") + " (" + probes[i] + ")");
			} else {
				probesList.addItem(probes[i]);
			}
		}
	}

	@Override
	public void sampleClassChanged(SampleClass sc) {
		oracle.setFilter(sc);
		super.sampleClassChanged(sc);
		//TODO think about what to do for the probes here
		//probesChanged(new String[0]);
	}

	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		for (ListBox l : compoundLists) {
			l.clear();
			for (Group g : columns) {
				for (String cmp : g.getMajors(schema())) {
					l.addItem(cmp);
				}
			}
		}
	}

	/**
	 * The incoming probes signal will set the probes 
	 * well as call the outgoing signal.
	 */
	@Override
	public void probesChanged(String[] probes) {
		probesList.clear();
		for (String p : probes) {
			// TODO look up syms here? 
			probesList.addItem(p);
		}
		listedProbes.clear();
		listedProbes.addAll(Arrays.asList(probes));
		updateProceedButton();
		super.probesChanged(probes); // calls changeProbes
	}
	
	@Override
	public void itemListsChanged(List<ItemList> lists) {
		super.itemListsChanged(lists);
		listChooser.setLists(lists);
	}

	private void updateProceedButton() {
		proceedSelected.setEnabled(listedProbes.size() > 0);
		proceedSelected.setText("Proceed with " + listedProbes.size()
				+ " selected probes >>");
	}

	/**
	 * The "outgoing" probes signal will assume that any widget state changes
	 * have already been done and store the probes.
	 */
	@Override
	public void changeProbes(String[] probes) {
		super.changeProbes(probes);
		StorageParser p = getParser(this);
		storeProbes(p);
	}

	@Override
	public void resizeInterface() {
		/*
		 * Test carefully in IE8, IE9 and all other browsers if changing this
		 * method. Compare with ColumnScreen for a related method. Future:
		 * extract this kind of functionality to a separate class, for example
		 * ResizingDockLayoutPanel.
		 */

		plPanel.setWidgetSize(plNorth, PL_NORTH_HEIGHT);
		plPanel.setWidgetSize(plSouth, PL_SOUTH_HEIGHT);
		plPanel.forceLayout();
		super.resizeInterface();
	}

	@Override
	public String getGuideText() {
		return "If you wish, you can select specific probes to inspect here. To to see all probes, use the second button at the bottom.";
	}

}
