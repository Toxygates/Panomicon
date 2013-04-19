package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import bioweb.shared.array.DataColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

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
	
	public ProbeScreen(ScreenManager man) {
		super("Probe selection", key, true, true, man,
				resources.probeSelectionHTML(), resources.probeSelectionHelp());				
	}

	@Override
	public boolean enabled() {
		return manager.isConfigured(ColumnScreen.key);
	}

	private ProbeSelector pathwaySel, gotermSel;

	public Widget content() {
		HorizontalPanel hp = Utils.mkHorizontalPanel();				
		hp.setSpacing(10);
//		hp.setHeight("492px");
		
		StackPanel probeSelStack = new StackPanel() {
			// This is to fix a height bug on IE8 - see
			// http://code.google.com/p/google-web-toolkit/issues/detail?id=2593
			// Future: use StackLayoutPanel instead!
			@Override
			protected void insert(Widget child, Element container, int beforeIndex,	boolean domInsert) {
				super.insert(child, container, beforeIndex, domInsert);
				DOM.removeElementAttribute(container, "height");
			}
		};
		
		hp.add(probeSelStack);
		probeSelStack.setSize("350px", "492px");

		pathwaySel = new ProbeSelector(
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
		probeSelStack.add(pathwaySel, "KEGG pathway search", false);
		pathwaySel.setWidth("100%");
		addListener(pathwaySel);
		
		gotermSel = new ProbeSelector(
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
		probeSelStack.add(gotermSel, "GO term search", false);
		pathwaySel.setWidth("100%");		
		addListener(gotermSel);		

		Widget chembl = makeTargetLookupPanel(
				"CHEMBL",
				"This lets you view probes that are known targets of the currently selected compound.");
		probeSelStack.add(chembl, "CHEMBL targets", false);

		Widget drugBank = makeTargetLookupPanel(
				"DrugBank",
				"This lets you view probes that are known targets of the currently selected compound.");
		probeSelStack.add(drugBank, "DrugBank targets", false);

		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		probeSelStack.add(verticalPanel_3, "Free selection", false);
		verticalPanel_3.setSize("100%", "");

		VerticalPanel vpi = Utils.mkVerticalPanel();
		vpi.setStyleName("colored");
		verticalPanel_3.add(vpi);
		vpi.setWidth("100%");
		
		Label label_5 = new Label(
				"Enter a list of probes, genes or proteins, one per line, to display only those.");
		label_5.setStyleName("none");
		vpi.add(label_5);

		customProbeText = new TextArea();
		vpi.add(customProbeText);
		customProbeText.setWidth("95%");
		
		vpi.add(new Button("Add manual list", new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String text = customProbeText.getText();
				String[] split = text.split("\n");
				
				if (split.length == 0) {
					Window.alert("Please enter probes, genes or proteins in the text box and try again.");
				} else {
					addManualProbes(split);					
				} }
		}));
		
		vpi = Utils.mkVerticalPanel();
		vpi.setStyleName("colored2");
		verticalPanel_3.add(vpi);
		vpi.setWidth("100%");
		
		Label l = new Label("Begin typing a gene name to get suggestions.");
		vpi.add(l);
		
		vpi.add(sb);
		sb.setWidth("95%");		
		vpi.add(new Button("Add gene", new ClickHandler() {
			public void onClick(ClickEvent ev) {
				String[] gs = new String[1];
				if (sb.getText().length() == 0) {
					Window.alert("Please type a gene name or identifier and try again.");
				}
				gs[0] = sb.getText();
				addManualProbes(gs);
			}
		}));
		
		VerticalPanel lp = Utils.mkVerticalPanel();
		l = new Label("Selected probes");
		l.setStyleName("heading");
		lp.add(l);
		probesList = new ListBox();
		probesList.setVisibleItemCount(25);
		probesList.setWidth("350px");
		lp.add(probesList);
		hp.add(lp);
		
		lp.add(new Button("Clear selected probes", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {				
				probesChanged(new String[0]);								
			}
		}));

		HorizontalPanel hpo = Utils.mkWidePanel();
		hpo.setHeight("100%");
		hpo.add(hp);
		
		return hpo;			
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
							storeProbes();
							configuredProceed(DataScreen.key);
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
		// change the identifiers (which can be mixed format) into a
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
		VerticalPanel verticalPanel_2 = Utils.mkVerticalPanel(true);			
		verticalPanel_2.setSize("100%", "100px");		
		
		Label label_4 = new Label(label);
		verticalPanel_2.add(label_4);
		
		final ListBox compoundList = new ListBox();
		compoundLists.add(compoundList);
		verticalPanel_2.add(compoundList);
		
		Button button = new Button("Add direct targets >>", makeTargetLookupCH(compoundList, service, false));
		verticalPanel_2.add(button);
		
		button = new Button("Add inferred targets >>", makeTargetLookupCH(compoundList, service, true));
		verticalPanel_2.add(button);		
		
		return verticalPanel_2;
	}
	
	private void addProbes(String[] probes) {			
		for (String p: probes) {
			listedProbes.add(p);
		}
		final String[] probesInOrder = listedProbes.toArray(new String[0]);
		chosenProbes = probesInOrder;
		storeProbes();
		
		if (probes.length > 0) {
			//TODO reduce the number of ajax calls done by this screen by collapsing  them
			owlimService.geneSyms(probesInOrder, chosenDataFilter, 
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
	 * deferred add probes. Probes must be unique.
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
//		
//		probesList.clear();
//		listedProbes.clear();
	}
	
	@Override
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		for (ListBox l: compoundLists) {
			l.clear();
			for (DataColumn c : columns) {
				Group g = (Group) c;
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
		storeProbes();
	}

}
