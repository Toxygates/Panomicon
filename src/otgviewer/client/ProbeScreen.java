package otgviewer.client;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
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

	public ProbeScreen(Screen parent) {
		super(parent, "Select probes", key, true);
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

			public void probesChanged(String[] probes) {
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
				owlimService.probesForGoTerm(item, retrieveProbesCallback());
			}

			public void probesChanged(String[] probes) {
				addProbes(probes);
			}
		};
		probeSelStack.add(gotermSel, "GO term search", false);
		gotermSel.setSize("100%", "");
		addListener(gotermSel);		

		Widget chembl = makeTargetLookupPanel(
				"CHEMBL",
				"This lets you view probes that are known targets of the currently selected compound.",
				"Show CHEMBL targets");
		probeSelStack.add(chembl, "CHEMBL targets", false);

		Widget drugBank = makeTargetLookupPanel(
				"DrugBank",
				"This lets you view probes that are known targets of the currently selected compound.",
				"Show DrugBank targets");
		probeSelStack.add(drugBank, "DrugBank targets", false);

		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		probeSelStack.add(verticalPanel_3, "Custom", false);
		verticalPanel_3.setSize("100%", "");

		Label label_5 = new Label(
				"Enter a list of probes, genes or proteins to display only those.");
		label_5.setStyleName("none");
		verticalPanel_3.add(label_5);

		customProbeText = new TextArea();
		verticalPanel_3.add(customProbeText);
		customProbeText.setSize("95%", "");

		Button button_1 = new Button("Add custom probes");
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
									addProbes(probes);
								}

								public void onFailure(Throwable caught) {

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
				probesList.clear();				
				listedProbes.clear();
			}
		});

		HorizontalPanel buttons = new HorizontalPanel();
		b = new Button("Display data with chosen probes");
		buttons.add(b);
		
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				changeProbes(listedProbes.toArray(new String[0]));
				History.newItem(DataScreen.key);
			}
		});

		b = new Button("Display data with all probes");
		buttons.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				changeProbes(new String[0]);
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
		
		Button button = new Button(buttonText);
		verticalPanel_2.add(button);
		button.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ev) {
				if (chosenCompound != null) {
					
					owlimService.probesTargetedByCompound(chosenDataFilter,
							chosenCompound, service, new AsyncCallback<String[]>() {
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
		probesList.clear();
		for (String p: listedProbes) {
			probesList.addItem(p);
		}		
	}
}
