/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.GeneOracle;
import otgviewer.client.ProbeSelector;
import t.common.client.components.ResizingDockLayoutPanel;
import t.common.client.components.ResizingListBox;
import t.common.shared.ItemList;
import t.common.shared.SharedUtils;
import t.common.shared.Term;
import t.viewer.client.Utils;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
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

public class ProbeSetEditor extends DataListenerWidget {

  private static final String NEW_TITLE_PREFIX = "NewProbeSet";

  private DialogBox dialog;

  private final Screen screen;

  private final SparqlServiceAsync sparqlService;
  private final MatrixServiceAsync matrixService;

  private final GeneOracle oracle;

  private Set<String> listedProbes = new HashSet<String>();

  private ListBox probesList;
  private TextArea customProbeText;
  private List<ListBox> compoundLists = new ArrayList<ListBox>();
  private DockLayoutPanel plPanel;
  private Widget plNorth, plSouth;
  private FixedWidthLayoutPanel fwlp;

  private TextBox probeSetTitle;

  private static final int STACK_WIDTH = 350;
  private static final int STACK_ITEM_HEIGHT = 29;
  private static final int PL_NORTH_HEIGHT = 30;
  private static final int PL_SOUTH_HEIGHT = 40;

  public ProbeSetEditor(Screen screen) {
    super();

    this.screen = screen;

    dialog = new DialogBox();
    oracle = new GeneOracle(screen);
    sparqlService = screen.manager.sparqlService();
    matrixService = screen.manager.matrixService();

    initWindow();
    // loadState(screen);

    System.out.println("Parent: " + screen.chosenProbes.length);
    System.out.println("Child: " + this.chosenProbes.length);
  }

  protected boolean hasChembl() {
    return true;
  }

  protected boolean hasDrugbank() {
    return true;
  }

  protected boolean hasSymbolFinder() {
    return true;
  }

  protected boolean hasPartialMatcher() {
    return false;
  }

  private void initWindow() {
    StackLayoutPanel probeSelStack = new StackLayoutPanel(Unit.PX);
    probeSelStack.setWidth(STACK_WIDTH + "px");

    ProbeSelector psel = probeSelector();
    probeSelStack.add(psel, "Keyword search", STACK_ITEM_HEIGHT);
    addListener(psel);

    if (hasChembl()) {
      Widget chembl =
          makeTargetLookupPanel(
              "CHEMBL",
              "This lets you view probes that are known targets of the currently selected compound.");
      probeSelStack.add(chembl, "CHEMBL targets", STACK_ITEM_HEIGHT);
    }

    if (hasDrugbank()) {
      Widget drugBank =
          makeTargetLookupPanel(
              "DrugBank",
              "This lets you view probes that are known targets of the currently selected compound.");
      probeSelStack.add(drugBank, "DrugBank targets", STACK_ITEM_HEIGHT);
    }

    probeSelStack.add(manualSelection(), "Free selection", STACK_ITEM_HEIGHT);

    Label l = new Label("Selected probes");
    l.setStylePrimaryName("heading");

    probesList = new ResizingListBox(74);
    probesList.setWidth("100%");

    HorizontalPanel buttons = Utils.mkHorizontalPanel(true);
    Button b = new Button("Clear selected probes", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        probesChanged(new String[0]);
      }
    });

    // final ProbeScreen ps = (ProbeScreen)screen;
    buttons.add(b);
    // buttons.getElement().getStyle().setMargin(100, Unit.PX);

    plPanel = new ResizingDockLayoutPanel();
    plNorth = Utils.wideCentered(l);
    plSouth = Utils.wideCentered(buttons);

    plPanel.addNorth(plNorth, PL_NORTH_HEIGHT);
    plPanel.addSouth(plSouth, PL_SOUTH_HEIGHT);

    plPanel.add(probesList);

    DockLayoutPanel dp = new ResizingDockLayoutPanel();
    dp.addWest(probeSelStack, STACK_WIDTH);
    dp.add(plPanel);

    FixedWidthLayoutPanel fwlp = new FixedWidthLayoutPanel(dp, 700, 0);
    // fwlp.setSize("100%", "100%");
    fwlp.setPixelSize(700, 500);

    HorizontalPanel bottomContent = new HorizontalPanel();
    // bottomContent.setStylePrimaryName("slightlySpaced");
    bottomContent.setSpacing(4);

    Button btnCancel = new Button("Cancel");
    btnCancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ProbeSetEditor.this.dialog.hide();
      }
    });
    Button btnSave = new Button("Save");
    btnSave.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // TODO
        ProbeSetEditor.this.dialog.hide();
      }
    });
    bottomContent.add(btnCancel);
    bottomContent.add(btnSave);

    HorizontalPanel bottomContainer = new HorizontalPanel();
    bottomContainer.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    // To ensure that child elements are right-aligned
    bottomContainer.setWidth("100%");
    bottomContainer.add(bottomContent);

    l = new Label("Title:");
    l.setStylePrimaryName("heading");
    l.addStyleName("table-cell");

    probeSetTitle = new TextBox();
    probeSetTitle.setWidth("100%");

    FlowPanel p = new FlowPanel();
    p.setWidth("100%");
    p.addStyleName("table-cell width-fix");
    p.add(probeSetTitle);

    FlowPanel topContent = new FlowPanel();
    topContent.add(l);
    topContent.add(p);

    // VerticalPanel content =
    // Utils.mkVerticalPanel(true, topContent, fwlp, bottomContainer);
    VerticalPanel content = new VerticalPanel();
    content.add(topContent);
    content.add(fwlp);
    content.add(bottomContainer);
    // content.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);

    dialog.setText("Probe set editor");
    dialog.setWidget(content);
    dialog.setGlassEnabled(true);
    dialog.setModal(true);
    dialog.center();
  }

  private ProbeSelector probeSelector() {
    return new ProbeSelector(
        screen,
        "This lets you view probes that correspond to a given KEGG pathway or GO term. "
            + "Enter a partial pathway name and press enter to search.", true) {

      @Override
      protected void getProbes(Term term) {
        sparqlService.probesForPathway(chosenSampleClass, term.getTermString(),
            getAllSamples(), retrieveProbesCallback());
        // TODO for GO
        // sparqlService.goTerms(pattern, retrieveMatchesCallback());
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
          Window
              .alert("Please enter identifiers in the text box and try again.");
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

      // TODO "filter" function as well as "add"
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

  /**
   * Obtain the gene symbols of the requested probes, then add them and display them. Probes must be
   * unique.
   * 
   * @param probes
   */
  private void addProbes(String[] probes) {
    for (String p : probes) {
      listedProbes.add(p);
    }
    // listChooser.setItems(new ArrayList<String>(listedProbes));

    final String[] probesInOrder = listedProbes.toArray(new String[0]);
    // chosenProbes = probesInOrder;
    // StorageParser p = screen.getParser(screen);
    // screen.storeProbes(p);

    if (probes.length > 0) {
      // TODO reduce the number of ajax calls done by this screen by
      // collapsing them
      sparqlService.geneSyms(probesInOrder, new AsyncCallback<String[][]>() {
        public void onSuccess(String[][] syms) {
          deferredAddProbes(probesInOrder, syms);
        }

        public void onFailure(Throwable caught) {
          Window.alert("Unable to get gene symbols for probes.");
        }
      });
    }
    // updateProceedButton();
  }

  private void addManualProbes(String[] probes, boolean titleMatch) {
    // change the identifiers (which can be mixed format, for example genes
    // and proteins etc) into a
    // homogenous format (probes only)
    matrixService.identifiersToProbes(probes, true, titleMatch, screen
        .getAllSamples(), new PendingAsyncCallback<String[]>(screen,
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
        probesList.addItem(SharedUtils.mkString(syms[i], "/") + " ("
            + probes[i] + ")");
      } else {
        probesList.addItem(probes[i]);
      }
    }
  }

  private ClickHandler makeTargetLookupCH(final ListBox compoundList,
      final String service, final boolean homologs) {
    final DataListenerWidget w = screen;
    return new ClickHandler() {
      public void onClick(ClickEvent ev) {
        if (compoundList.getSelectedIndex() != -1) {
          String compound =
              compoundList.getItemText(compoundList.getSelectedIndex());
          sparqlService.probesTargetedByCompound(screen.chosenSampleClass,
              compound, service, homologs, new PendingAsyncCallback<String[]>(
                  w, "Unable to get probes (technical error).") {
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

    Button button =
        new Button("Add direct targets >>", makeTargetLookupCH(compoundList,
            service, false));
    vpi.add(button);

    button =
        new Button("Add inferred targets >>", makeTargetLookupCH(compoundList,
            service, true));
    vpi.add(button);

    vp.add(vpi);
    return vp;
  }

  /**
   * The incoming probes signal will set the probes well as call the outgoing signal.
   */
  public void probesChanged(String[] probes) {
    System.out.println("probesChanged: " + probes.length);

    probesList.clear();
    for (String p : probes) {
      // TODO look up syms here?
      probesList.addItem(p);
    }
    listedProbes.clear();
    listedProbes.addAll(Arrays.asList(probes));

    super.probesChanged(probes); // calls changeProbes
  }

  public void createNew() {
    probeSetTitle.setText(getAvailableName());

    // Create temporal DataListenerWidget to avoid loading probes chosen in parent screen
    DataListenerWidget dlw = new DataListenerWidget();
    screen.propagateTo(dlw);
    dlw.probesChanged(new String[0]);
    dlw.propagateTo(this);

    dialog.show();
  }

  public void edit(String name) {
    probeSetTitle.setText(name);
    screen.propagateTo(this);
    dialog.show();
  }

  private String getAvailableName() {
    String newTitle = NEW_TITLE_PREFIX;

    int i = 1;
    while (isExist(newTitle)) {
      newTitle = NEW_TITLE_PREFIX + " " + (++i);
    }

    return newTitle;
  }

  private boolean isExist(String name) {
    for (ItemList li : chosenItemLists) {
      if (!li.type().equals("probes")) {
        continue;
      }
      if (li.name().equals(name)) {
        return true;
      }
    }

    return false;
  }

}
