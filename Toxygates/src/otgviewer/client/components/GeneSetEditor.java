/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import static t.common.client.Utils.makeButton;

import java.util.*;
import java.util.stream.Stream;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.*;
import otgviewer.client.rpc.ProbeServiceAsync;
import t.common.client.components.ResizingDockLayoutPanel;
import t.common.client.components.ResizingListBox;
import t.common.shared.*;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;

public class GeneSetEditor extends DataListenerWidget implements HasSaveActionHandler {

  private static final String NEW_TITLE_PREFIX = "NewGeneSet";

  public static final int SAVE_FAILURE = -1;
  public static final int SAVE_SUCCESS = 0;

  private DialogBox dialog;

  private final ImportingScreen screen;

  private final ProbeServiceAsync probeService;

  private final GeneOracle oracle;

  private Set<String> originalProbes;
  private Set<String> listedProbes = new HashSet<String>();

  private ListBox probesList;
  private final ListBox compoundList = new ListBox();
  private TextArea customProbeText;
  private DockLayoutPanel plPanel;
  private Widget plNorth, plSouth;

  private String originalTitle;
  private Boolean editingExistingGeneSet;
  private TextBox titleText;

  private RadioButton chembl;
  private RadioButton drugbank;

  private static final int STACK_WIDTH = 350;
  protected static final int STACK_ITEM_HEIGHT = 29;
  private static final int PL_NORTH_HEIGHT = 30;
  private static final int PL_SOUTH_HEIGHT = 40;

  private List<SaveActionHandler> saveActions = new ArrayList<SaveActionHandler>();

  public GeneSetEditor(ImportingScreen screen) {
    super();

    this.screen = screen;
    dialog = new DialogBox();
    oracle = new GeneOracle(screen);
    probeService = screen.manager().probeService();

    initWindow();
  }

  /**
   * Construct a gene set editor in a DataScreen and set up listeners
   * appropriately.
   * @param screen
   * @param parent
   * @return
   */
  public static GeneSetEditor make(final DataScreen screen, final DataListenerWidget parent) {
    GeneSetEditor gse = screen.factory().geneSetEditor(screen);
    gse.addSaveActionHandler(new SaveActionHandler() {
      @Override
      public void onSaved(String title, List<String> items) {
        String[] itemsArray = items.toArray(new String[0]);
        screen.geneSetChanged(new StringList(StringList.PROBES_LIST_TYPE, 
            title, itemsArray));
        screen.probesChanged(itemsArray);
        screen.updateProbes();
      }

      @Override
      public void onCanceled() {}
    });
    parent.addListener(gse);
    return gse;
  }
  
  /*
   * Override this function to handle post save event and what genes were saved
   */
  protected void onSaved(String title, List<String> items) {}

  /*
   * Override this function to handle what genes were saved
   */
  protected void onCanceled() {}

  protected boolean hasChembl() {
    return true;
  }

  protected boolean hasDrugbank() {
    return true;
  }

  protected boolean hasClustering() {
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
    addProbeSelectionTools(probeSelStack);

    Label l = new Label("Selected probes");
    l.addStyleName("heading");

    probesList = new ResizingListBox(74);
    probesList.setMultipleSelect(true);
    probesList.setWidth("100%");

    HorizontalPanel buttons = Utils.mkHorizontalPanel(true);
    Button removeSelected = makeButton("Remove selected probes", () -> {      
        for (int i = 0; i < probesList.getItemCount(); ++i) {
          if (probesList.isItemSelected(i)) {
            String sel = probesList.getItemText(i);
            int from = sel.lastIndexOf('(');
            int to = sel.lastIndexOf(')');
            if (from != -1 && to != -1) {
              sel = sel.substring(from + 1, to);
            }
            listedProbes.remove(sel);
          }
        }

        probesChanged(listedProbes.toArray(new String[0]));
      });
    
    Button removeAll = makeButton("Remove all probes", () -> probesChanged(new String[0]));

    buttons.add(removeSelected);
    buttons.add(removeAll);

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
    fwlp.setPixelSize(700, 500);

    HorizontalPanel bottomContent = new HorizontalPanel();
    bottomContent.setSpacing(4);

    Button btnCancel = makeButton("Cancel", () -> {
      if (!listedProbes.equals(originalProbes)) {
        // TODO Need to confirm if lists are not saved?
      }

      GeneSetEditor.this.dialog.hide();
      for (SaveActionHandler h : saveActions) {
        h.onCanceled();
      }
    });
    
    Button btnSave = makeButton("Save", () -> {
      String title = titleText.getText().trim();

      if (save(title)) {
        GeneSetEditor.this.dialog.hide();
        for (SaveActionHandler h : saveActions) {
          h.onSaved(title, new ArrayList<String>(listedProbes));
        }
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
    l.addStyleName("heading");
    l.addStyleName("table-cell");

    titleText = new TextBox();
    titleText.setWidth("100%");

    FlowPanel p = new FlowPanel();
    p.setWidth("100%");
    p.addStyleName("table-cell width-fix");
    p.add(titleText);

    FlowPanel topContent = new FlowPanel();
    topContent.add(l);
    topContent.add(p);

    VerticalPanel content = new VerticalPanel();
    content.add(topContent);
    content.add(fwlp);
    content.add(bottomContainer);

    dialog.setText("Gene set editor");
    dialog.setWidget(content);
    dialog.setGlassEnabled(true);
    dialog.setModal(true);
    dialog.center();
  }

  protected void addProbeSelectionTools(StackLayoutPanel probeSelStack) {
    ProbeSelector psel = probeSelector();
    probeSelStack.add(psel, "Keyword search", STACK_ITEM_HEIGHT);
    addListener(psel);

    if (hasChembl() || hasDrugbank()) {
      Widget targets =
          makeTargetLookupPanel("This lets you view probes that are known targets of the currently selected compound.");
      probeSelStack.add(targets, "Targets", STACK_ITEM_HEIGHT);
    }

    if (hasClustering()) {
      ClusteringSelector clustering = clusteringSelector();
      probeSelStack.add(clustering, "Clustering", STACK_ITEM_HEIGHT);
    }

    probeSelStack.add(manualSelection(), "Free selection", STACK_ITEM_HEIGHT);

  }

  private boolean save(String name) {
    StringListsStoreHelper helper = new StringListsStoreHelper("probes", screen);
    boolean overwrite = false;

    if (editingExistingGeneSet && name.equals(originalTitle)) {
      overwrite = true;
    }

    if (helper.saveAs(new ArrayList<String>(listedProbes), name, overwrite)) {
      if (overwrite) {
        Analytics.trackEvent(Analytics.CATEGORY_GENE_SET,
            Analytics.ACTION_MODIFY_EXISTING_GENE_SET);

      } else {
        Analytics.trackEvent(Analytics.CATEGORY_GENE_SET, Analytics.ACTION_CREATE_NEW_GENE_SET);
      }
      return true;
    } else {
      return false;
    }
  }

  private ProbeSelector probeSelector() {
    return new ProbeSelector(screen,
        "This lets you view probes that correspond to a given KEGG pathway or GO term. "
            + "Enter a partial pathway name and press enter to search.", true) {

      @Override
      protected void getProbes(Term term) {
        switch (term.getAssociation()) {
          case KEGG:
            probeService.probesForPathway(term.getTermString(),
                getAllSamples(), retrieveProbesCallback());
            break;
          case GO:
            probeService.probesForGoTerm(term.getTermString(), getAllSamples(),
                retrieveProbesCallback());
            break;
          default:
        }
      }

      @Override
      public void probesChanged(String[] probes) {
        super.probesChanged(probes);
        addProbes(probes);
      }
    };
  }

  private ClusteringSelector clusteringSelector() {
    return new ClusteringSelector(screen.appInfo().probeClusterings()) {
      @Override
      public void clusterChanged(List<String> items) {
        addProbes(items.toArray(new String[0]));
      }
    };
  }

  private VerticalPanel innerVP(String l) {
    VerticalPanel vpii = Utils.mkVerticalPanel();
    vpii.setWidth("100%");
    vpii.addStyleName("geneSetInnerPanel");

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

    vpii.add(makeButton("Add manual list", () -> {
      String text = customProbeText.getText();
      String[] split = text.split("[\n ,\t]");

      if (split.length == 0) {
        Window.alert("Please enter identifiers in the text box and try again.");
      } else {
        addManualProbes(split, false);
      }
    }));
    

    if (hasSymbolFinder()) {
      vpii = innerVP("Begin typing a gene symbol to get suggestions.");
      vpi.add(vpii);

      final SuggestBox sb = new SuggestBox(oracle);
      vpii.add(sb);
      sb.setWidth("95%");
      vpii.add(makeButton("Add gene", () -> {
        String[] gs = new String[1];
        if (sb.getText().length() == 0) {
          Window.alert("Please enter a gene symbol and try again.");
        }
        gs[0] = sb.getText();
        addManualProbes(gs, false);
      }));      
    }

    if (hasPartialMatcher()) {
      vpii = innerVP("Match by partial probe name:");
      vpi.add(vpii);

      //In the future, we might have a "filter" function for intersection,
      //in addition to "add" (which is effectively a union)
      final TextBox tb = new TextBox();
      vpii.add(tb);
      tb.setWidth("95%");
      vpii.add(makeButton("Add", () -> {
        String[] gs = new String[1];
        if (tb.getText().length() == 0) {
          Window.alert("Please enter a pattern and try again.");
        }
        gs[0] = tb.getText();
        addManualProbes(gs, true);
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
  protected void addProbes(String[] probes) {
    for (String p : probes) {
      listedProbes.add(p);
    }

    final String[] probesInOrder = listedProbes.toArray(new String[0]);

    if (probes.length > 0) {
      // TODO reduce the number of ajax calls done by this screen by
      // collapsing them
      probeService.geneSyms(probesInOrder, new AsyncCallback<String[][]>() {
        @Override
        public void onSuccess(String[][] syms) {
          deferredAddProbes(probesInOrder, syms);
        }

        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Unable to get gene symbols for probes.");
        }
      });
    }
    // updateProceedButton();
  }

  protected void addManualProbes(String[] probes, boolean titleMatch) {
    // change the identifiers (which can be mixed format, for example genes
    // and proteins etc) into a
    // homogenous format (probes only)
    probeService.identifiersToProbes(probes, true, false, titleMatch, 
        screen.getAllSamples(),
        new PendingAsyncCallback<String[]>(screen,
            "Unable to obtain manual probes (technical error).") {
          @Override
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
        probesList.addItem(SharedUtils.mkString(syms[i], "/") + " (" + probes[i] + ")");
      } else {
        probesList.addItem(probes[i]);
      }
    }
  }

  private void doTargetLookup(final String service, final boolean homologs) {
    final Screen w = screen;
    if (compoundList.getSelectedIndex() != -1) {
      String compound = compoundList.getItemText(compoundList.getSelectedIndex());

      // Used for organism - TODO fix this for multi-organism cases
      SampleClass sc = screen.chosenColumns().get(0).samples()[0].sampleClass();
      logger.info("Target lookup for: " + sc.toString());

      probeService.probesTargetedByCompound(sc, compound, service, homologs,
          new PendingAsyncCallback<String[]>(w, "Unable to get probes (technical error).") {
            @Override
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

  private Widget makeTargetLookupPanel(String label) {
    VerticalPanel vp = new VerticalPanel();
    vp.setSize("100%", "100%");
    vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

    VerticalPanel vpi = Utils.mkVerticalPanel(true);
    vpi.addStyleName("colored");
    Label l = new Label(label);
    vpi.add(l);

    HorizontalPanel hp = Utils.mkHorizontalPanel(true);
    // TODO use Enum to reduce if-sentence
    if (hasChembl()) {
      chembl = new RadioButton("Target", "CHEMBL");
      hp.add(chembl);
    }
    if (hasDrugbank()) {
      drugbank = new RadioButton("Target", "DrugBank");
      hp.add(drugbank);
    }
    selectDefaultTargets();
    vpi.add(hp);

    vpi.add(compoundList);

    Button button = makeButton("Add direct targets >>", () -> {    
        System.out.println(selectedTarget() + " selected");
        doTargetLookup(selectedTarget(), false);
      });    
    vpi.add(button);

    button = makeButton("Add inferred targets >>", () -> {
      System.out.println(selectedTarget() + " selected");
      doTargetLookup(selectedTarget(), true);
    });   

    vpi.add(button);

    vp.add(vpi);
    return vp;
  }

  private void selectDefaultTargets() {
    if (chembl != null) {
      chembl.setValue(true);
      return;
    }
    if (drugbank != null) {
      drugbank.setValue(true);
      return;
    }
  }

  private String selectedTarget() {
    if (chembl != null && chembl.getValue()) {
      return "CHEMBL";
    }
    if (drugbank != null && drugbank.getValue()) {
      return "DrugBank";
    }

    return null;
  }

  /**
   * The incoming probes signal will set the probes well as call the outgoing signal.
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

    super.probesChanged(probes); // calls changeProbes
  }

  @Override
  public void columnsChanged(List<Group> cs) {
    super.columnsChanged(cs);
    Stream<String> compounds = Group.collectAll(cs, screen.schema().majorParameter());
    compoundList.clear();
    compounds.forEach(c -> compoundList.addItem(c));    
  }

  public void createNew(String[] initProbes) {
    // Create temporary DataListenerWidget to avoid loading probes chosen in parent screen
    DataListenerWidget dlw = new DataListenerWidget();
    screen.propagateTo(dlw);
    dlw.probesChanged(initProbes);
    dlw.propagateTo(this);

    originalProbes = null;
    originalTitle = getAvailableName();
    editingExistingGeneSet = false;
    titleText.setText(originalTitle);
    dialog.show();
  }

  public void edit(String name) {
    screen.propagateTo(this);

    originalProbes = new HashSet<String>(listedProbes);
    originalTitle = name;
    editingExistingGeneSet = true;
    titleText.setText(originalTitle);
    dialog.show();
  }

  private String getAvailableName() {
    String newTitle = NEW_TITLE_PREFIX;
    System.out.println("Existing names");
    for (ItemList li : chosenItemLists) {
      System.out.println(li.name());
    }

    int i = 1;
    while (exists(newTitle)) {
      newTitle = NEW_TITLE_PREFIX + " " + (++i);
    }

    return newTitle;
  }

  private boolean exists(String name) {
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

  @Override
  public void addSaveActionHandler(SaveActionHandler handler) {
    this.saveActions.add(handler);
  }

}
