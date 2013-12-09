package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.FixedWidthLayoutPanel;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.ResizingDockLayoutPanel;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.storage.client.Storage;
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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The probe selection screen.
 */
public class ProbeScreen extends Screen {

	public static final String key = "probes";
	private SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);
	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);


	private TextArea customProbeText;
	private ListBox probesList;
	private Set<String> listedProbes = new HashSet<String>();
	private List<ListBox> compoundLists = new ArrayList<ListBox>();
	final GeneOracle oracle = new GeneOracle();
	final SuggestBox sb = new SuggestBox(oracle);
	private Button proceedSelected;
	
	private static final int STACK_ITEM_HEIGHT = 29;
	
	public ProbeScreen(ScreenManager man) {
		super("Probe selection", key, true, true, man,
				resources.probeSelectionHTML(), resources.probeSelectionHelp());				
	}

	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key);
	}

	private ProbeSelector pathwaySelector() {
		return new ProbeSelector(
				"This lets you view probes that correspond to a given KEGG pathway. "
						+ "Enter a partial pathway name and press enter to search.", true) {
			protected void getMatches(String pattern) {
				owlimService.pathways(chosenDataFilter, pattern,
						retrieveMatchesCallback());
			}

			protected void getProbes(String item) {
				owlimService.probesForPathway(chosenDataFilter, item,
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
							+ "Enter a partial term name and press enter to search.", true) {
				protected void getMatches(String pattern) {
					owlimService.goTerms(pattern, retrieveMatchesCallback());
				}

				protected void getProbes(String item) {
					owlimService.probesForGoTerm(chosenDataFilter, item, retrieveProbesCallback());
				}
				
				@Override
				public void probesChanged(String[] probes) {
					super.probesChanged(probes);
					addProbes(probes);
				}
			};
	}
	
	private Widget manualSelection() {
		VerticalPanel vp = new VerticalPanel();		
		vp.setSize("100%", "100%");
		vp.setSpacing(5);
		vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		VerticalPanel vpi = Utils.mkVerticalPanel();
		vpi.setWidth("100%");
		vp.add(vpi);
		
		VerticalPanel vpii = Utils.mkVerticalPanel();
		vpii.setWidth("100%");
		vpii.setStyleName("colored");
		vpi.add(vpii);
		
		Label label = new Label(
				"Enter a list of probes, genes or proteins, one per line, to display only those.");
		label.setStyleName("none");
		vpii.add(label);

		customProbeText = new TextArea();
		vpi.add(customProbeText);
		customProbeText.setVisibleLines(10);
		customProbeText.setWidth("95%");
		
		vpii.add(new Button("Add manual list", new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String text = customProbeText.getText();
				String[] split = text.split("\n");
				
				if (split.length == 0) {
					Window.alert("Please enter probes, genes or proteins in the text box and try again.");
				} else {
					addManualProbes(split);					
				} }
		}));
		
		vpii = Utils.mkVerticalPanel();
		vpii.setStyleName("colored2");
		vpi.add(vpii);
		vpii.setWidth("100%");
		
		Label l = new Label("Begin typing a gene symbol to get suggestions.");
		vpii.add(l);
		
		vpii.add(sb);
		sb.setWidth("95%");		
		vpi.add(new Button("Add gene", new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String[] gs = new String[1];
				if (sb.getText().length() == 0) {
					Window.alert("Please type a gene symbol and try again.");
				}
				gs[0] = sb.getText();
				addManualProbes(gs);
			}
		}));
		return vp;
	}
	
	public Widget content() {		
		StackLayoutPanel probeSelStack = new StackLayoutPanel(Unit.PX);		
		probeSelStack.setWidth("350px");

		ProbeSelector psel = pathwaySelector();		
		probeSelStack.add(psel, "KEGG pathway search", STACK_ITEM_HEIGHT);
		addListener(psel);		
		psel = goTermSelector();
		probeSelStack.add(psel, "GO term search", STACK_ITEM_HEIGHT);
		addListener(psel);		

		Widget chembl = makeTargetLookupPanel(
				"CHEMBL",
				"This lets you view probes that are known targets of the currently selected compound.");
		probeSelStack.add(chembl, "CHEMBL targets", STACK_ITEM_HEIGHT);

		Widget drugBank = makeTargetLookupPanel(
				"DrugBank",
				"This lets you view probes that are known targets of the currently selected compound.");
		probeSelStack.add(drugBank, "DrugBank targets", STACK_ITEM_HEIGHT);

		probeSelStack.add(manualSelection(), "Free selection", STACK_ITEM_HEIGHT);
		
		DockLayoutPanel probeListPanel = new ResizingDockLayoutPanel();
		
		Label l = new Label("Selected probes");
		l.setStyleName("heading");		
		probeListPanel.addNorth(Utils.wideCentered(l), 10);

		probesList = new ListBox();
		probesList.setVisibleItemCount(15);
		probesList.setWidth("100%");

		Button b = new Button("Clear selected probes", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {				
				probesChanged(new String[0]);								
			}
		});
		probeListPanel.addSouth(Utils.wideCentered(b), 10);

		probeListPanel.add(probesList);
		
		DockLayoutPanel dp = new ResizingDockLayoutPanel();
		dp.addWest(probeSelStack, 100);
		dp.add(probeListPanel);
		
		FixedWidthLayoutPanel fwlp = new FixedWidthLayoutPanel(dp, 700, 10);
		fwlp.setSize("100%", "100%");
		
		return fwlp;
	}
	
	@Override
	public Widget bottomContent() {
		HorizontalPanel buttons = Utils.mkHorizontalPanel(false);
		proceedSelected = new Button("Proceed with selected probes",
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						if (listedProbes.size() == 0) {
							Window.alert("Please select the probes you are interested in, or proceed with all probes.");
						} else {
							chosenProbes = listedProbes.toArray(new String[0]);
							Storage s = tryGetStorage();
							if (s != null) {
								storeProbes(s);
								configuredProceed(DataScreen.key);
							}
						}
					}
				});
		buttons.add(proceedSelected);
		updateProceedButton();
		
		buttons.add(new Button("Proceed with all probes", new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (listedProbes.size() == 0 || Window.confirm("Proceeding will erase your list of " + listedProbes.size() + " selected probes.")) {
					probesChanged(new String[0]);
					configuredProceed(DataScreen.key);	
				}				
			}
		}));		
		return buttons;
	}
	
	private void addManualProbes(String[] probes) {
		// change the identifiers (which can be mixed format, for example genes and proteins etc) into a
		// homogenous format (probes only)
		kcService.identifiersToProbes(chosenDataFilter, probes, true, 
				new PendingAsyncCallback<String[]>(this, "Unable to obtain manual probes (technical error).") {
					public void handleSuccess(String[] probes) {
						if (probes.length == 0) {
							Window.alert("No matching probes were found.");
						} else {							
							addProbes(probes);
						}
					}
				});
	}
	
	private ClickHandler makeTargetLookupCH(final ListBox compoundList, final String service, 
			final boolean homologs) {
		final DataListenerWidget w = this;
		return new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (compoundList.getSelectedIndex() != -1) {
					String compound = compoundList.getItemText(compoundList.getSelectedIndex());					
					owlimService.probesTargetedByCompound(chosenDataFilter,
							compound, service, homologs, new PendingAsyncCallback<String[]>(w, "Unable to get probes (technical error).") {								
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
		vpi.setStyleName("colored");
		Label l = new Label(label);
		vpi.add(l);
		
		final ListBox compoundList = new ListBox();
		compoundLists.add(compoundList);
		vpi.add(compoundList);
		
		Button button = new Button("Add direct targets >>", makeTargetLookupCH(compoundList, service, false));
		vpi.add(button);
		
		button = new Button("Add inferred targets >>", makeTargetLookupCH(compoundList, service, true));
		vpi.add(button);
		
		vp.add(vpi);
		return vp;
	}
	
	/**
	 * Obtain the gene symbols of the requested probes, then add them and display them.
	 * Probes must be unique.
	 * @param probes
	 */
	private void addProbes(String[] probes) {			
		for (String p: probes) {
			listedProbes.add(p);
		}
		final String[] probesInOrder = listedProbes.toArray(new String[0]);
		chosenProbes = probesInOrder;
		Storage s = tryGetStorage();
		if (s != null) {
			storeProbes(s);

			if (probes.length > 0) {
				// TODO reduce the number of ajax calls done by this screen by
				// collapsing them
				owlimService.geneSyms(chosenDataFilter, probesInOrder, 
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

	}
	
	
	/**
	 * Display probes with gene symbols. Probes must be unique.
	 * @param probes
	 * @param syms
	 */
	private void deferredAddProbes(String[] probes, String[][] syms) {
		probesList.clear();
		for (int i = 0; i < probes.length; ++i) {
			if (syms[i].length > 0) {
				probesList.addItem(syms[i][0] + " (" + probes[i] + ")");
			} else {
				probesList.addItem(probes[i]);
			}			
		}		
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		oracle.setFilter(filter);
		if (chosenDataFilter != null && !filter.organism.equals(chosenDataFilter.organism)) {
			super.dataFilterChanged(filter);
			probesChanged(new String[0]);						
		} else {
			super.dataFilterChanged(filter);
		}
	}
	
	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		for (ListBox l: compoundLists) {
			l.clear();
			for (Group g : columns) {				
				for (String cmp: g.getCompounds()) {
					l.addItem(cmp);
				}
			}
		}		
	}

	/**
	 * The "incoming" probes signal will reset all the
	 * probes tracking state as well as call the outgoing signal.
	 */
	@Override
	public void probesChanged(String[] probes) {		
		probesList.clear();
		for (String p: probes) {
			probesList.addItem(p);
		}
		listedProbes.clear();
		listedProbes.addAll(Arrays.asList(probes));		
		updateProceedButton();
		super.probesChanged(probes); //calls changeProbes		
	}
	
	private void updateProceedButton() {
		proceedSelected.setEnabled(listedProbes.size() > 0);
		proceedSelected.setText("Proceed with " + listedProbes.size() + " selected probes >>");		
	}
	
	/**
	 * The "outgoing" probes signal will
	 * assume that any widget state changes have already been done
	 * and store the probes.
	 */
	@Override 
	public void changeProbes(String[] probes) {
		super.changeProbes(probes);
		Storage s = tryGetStorage();
		if (s != null) {
			storeProbes(s);
		}
	}
	
	@Override
	public String getGuideText() {
		return "If you want, you can select specific probes to inspect here. If you want to see all probes, use the second button at the bottom."; 
	}

}
