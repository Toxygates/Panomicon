package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ProbeScreen extends Screen {

	public static final String key = "probes";
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);


	private TextArea customProbeText;
	private ListBox probesList;
	private Set<String> listedProbes = new HashSet<String>();
	private List<ListBox> compoundLists = new ArrayList<ListBox>();
	
	public ProbeScreen(Screen parent, ScreenManager man) {
		super(parent, "Select probes", key, true, man);
	}

	private ProbeSelector pathwaySel, gotermSel;

	public Widget content() {
		HorizontalPanel hp = new HorizontalPanel();

		StackPanel probeSelStack = new StackPanel();
		hp.add(probeSelStack);
		probeSelStack.setSize("350px", "592px");

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
		pathwaySel.setSize("100%", "");
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
		gotermSel.setSize("100%", "");
		addListener(gotermSel);		

		Widget chembl = makeTargetLookupPanel(
				"CHEMBL",
				"This lets you view probes that are known targets of the currently selected compound.",
				"Add CHEMBL targets >>");
		probeSelStack.add(chembl, "CHEMBL targets", false);

		Widget drugBank = makeTargetLookupPanel(
				"DrugBank",
				"This lets you view probes that are known targets of the currently selected compound.",
				"Add DrugBank targets >>");
		probeSelStack.add(drugBank, "DrugBank targets", false);

		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		probeSelStack.add(verticalPanel_3, "Free selection", false);
		verticalPanel_3.setSize("100%", "");

		Label label_5 = new Label(
				"Enter a list of probes, genes or proteins, one per line, to display only those.");
		label_5.setStyleName("none");
		verticalPanel_3.add(label_5);

		customProbeText = new TextArea();
		verticalPanel_3.add(customProbeText);
		customProbeText.setSize("95%", "");

		Button button_1 = new Button("Add manual probes");
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
					kcService.identifiersToProbes(chosenDataFilter, split, true, 
							new AsyncCallback<String[]>() {
								public void onSuccess(String[] probes) {
									addProbes(probes);
								}

								public void onFailure(Throwable caught) {
									Window.alert("Unable to resolve manual probes");
								}
							});
				}
			}
		});
		
		VerticalPanel lp = new VerticalPanel();
		Label l = new Label("Selected probes");
		l.setStyleName("heading");
		lp.add(l);
		probesList = new ListBox();
		probesList.setVisibleItemCount(20);
		probesList.setWidth("350px");
		lp.add(probesList);
		hp.add(lp);
		
		lp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		Button b = new Button("Clear selected probes");
		lp.add(b);
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {				
				probesChanged(new String[0]);								
			}
		});

		HorizontalPanel buttons = new HorizontalPanel();
		b = new Button("Display data with chosen probes");
		buttons.add(b);
		
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				if (listedProbes.size() == 0) {
					Window.alert("Please select the probes you are interested in, or proceed with all probes.");
				} else {
					chosenProbes = listedProbes.toArray(new String[0]);					
					storeProbes();
					History.newItem(DataScreen.key);
				}
			}
		});

		b = new Button("Display data with all probes");
		buttons.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {				
				probesChanged(new String[0]);								
				History.newItem(DataScreen.key);
			}
		});
		
		dockPanel.add(buttons, DockPanel.SOUTH);
		return hp;
	}
	
	private Widget makeTargetLookupPanel(final String service, String label, String buttonText) {
		VerticalPanel verticalPanel_2 = new VerticalPanel();
		verticalPanel_2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);		
		verticalPanel_2.setSize("100%", "100px");		
		
		Label label_4 = new Label(label);
		verticalPanel_2.add(label_4);
		
		final ListBox compoundList = new ListBox();
		compoundLists.add(compoundList);
		verticalPanel_2.add(compoundList);
		
		Button button = new Button(buttonText);
		verticalPanel_2.add(button);
		button.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (compoundList.getSelectedIndex() != -1) {
					String compound = compoundList.getItemText(compoundList.getSelectedIndex());					
					owlimService.probesTargetedByCompound(chosenDataFilter,
							compound, service, new AsyncCallback<String[]>() {
								public void onFailure(Throwable caught) {
									Window.alert("Unable to get probes.");
								}

								public void onSuccess(String[] probes) {		
									addProbes(probes);
								}
							});
				} else {
					Window.alert("Please select a compound first.");
				}
			}
		});
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
			owlimService.geneSyms(probesInOrder, new AsyncCallback<String[][]>() {
				public void onSuccess(String[][] syms) {
					deferredAddProbes(probesInOrder, syms);
				}

				public void onFailure(Throwable caught) {
					Window.alert("Unable to get gene symbols for probes.");
				}
			});			
		}
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
	public void columnsChanged(List<DataColumn> columns) {
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
		super.probesChanged(probes);		
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
