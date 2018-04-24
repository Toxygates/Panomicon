package otgviewer.client;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.SingleSelectionModel;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.ScreenManager;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.model.sample.AttributeSet;
import t.viewer.client.StorageParser;
import t.viewer.client.Utils;
import t.viewer.client.network.NetworkController;
import t.viewer.client.network.NetworkViewer;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.table.*;
import t.viewer.shared.Association;
import t.viewer.shared.Synthetic;
import t.viewer.shared.network.*;

/**
 * A DataScreen that can display two tables side by side.
 * The dual mode will only activate if appropriate columns have been
 * defined and saved for both tables. Otherwise, the screen will revert to
 * single-table mode.
 * 
 * The "main" table drives the side table, in the sense that what is being displayed in the
 * latter depends on the content of the former.
 */
public class DualDataScreen extends DataScreen implements NetworkViewer {

  protected ExpressionTable sideExpressionTable;
  
  protected final static String sideMatrix = "SECONDARY";
  
  final static int MAX_SECONDARY_ROWS = Network.MAX_SIZE;
  
  static enum DualMode {
    Forward("mRNA", "miRNA", AType.MiRNA, true) {
      @Override
      void setVisibleColumns(ExpressionTable table) {
        table.setVisible(AType.MiRNA, true);
        table.setVisible(AType.MRNA, false);
      }
    },
    
    Reverse("miRNA", "mRNA", AType.MRNA, true) {
      @Override
      void setVisibleColumns(ExpressionTable table) {
        table.setVisible(AType.MiRNA, false);
        table.setVisible(AType.MRNA, true);
      }
    },
    SingleMRNA("mRNA", "miRNA", null, false), SingleMiRNA("miRNA", "mRNA", null, false);
    
    final String mainType, sideType;
    final AType linkingType;
    final boolean isSplit;
    DualMode(String mainType, String sideType, AType linkingType, boolean isSplit) {
      this.mainType = mainType;
      this.sideType = sideType;
      this.linkingType = linkingType;
      this.isSplit = isSplit;
    }
    
    int sideTableWidth() { 
      if (!isSplit) {
        return 0;
      } else if (sideType.equals("miRNA")) {
        return 550;
      } else {
        return 800;
      }
    }
    TableStyle mainStyle() { return TableStyle.getStyle(mainType); }
    TableStyle sideStyle() { return TableStyle.getStyle(sideType); }
    DualMode flip() { return (this == Forward) ? Reverse : Forward; }   
    
    void setVisibleColumns(ExpressionTable table) { }
  }
  
  /**
   * The preferred/default mode when two column types are available.
   */
  DualMode preferredDoubleMode = DualMode.Forward;
  
  /**
   * The current mode.
   */
  DualMode mode = DualMode.Forward;
  
  NetworkController controller = new NetworkController(this) {
    @Override
    public Map<String, Collection<String>> linkingMap() {
      return DualDataScreen.this.linkingMap();
    }    
  };
  
  public DualDataScreen(ScreenManager man) {
    super(man);
    
    TableFlags flags = new TableFlags(sideMatrix, true, false, 
        MAX_SECONDARY_ROWS, mode.sideType, true, true);
    sideExpressionTable = new ExpressionTable(this, flags,
        mode.sideStyle());     
    sideExpressionTable.addStyleName("sideExpressionTable");
    
    expressionTable.selectionModel().addSelectionChangeHandler(e ->
      onSourceSelectionChanged(getSelectedSourceNode()));
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e ->
      onDestSelectionChanged(getSelectedDestNode()));
  }
  
  protected void flipDualView() {    
    preferredDoubleMode = mode.flip();    
    List<Group> allColumns = new ArrayList<Group>(chosenColumns);
    allColumns.addAll(sideExpressionTable.chosenColumns());
    columnsChanged(allColumns);

    updateProbes();
  }
  
  protected SplitLayoutPanel splitLayout;
  
  /*
   * Note: It's probably best/easiest to rebuild the layout completely
   * each time the chosen columns change
   */
  @Override
  protected Widget mainTablePanel() {    
    splitLayout = new SplitLayoutPanel();    
    splitLayout.setWidth("100%");
    splitLayout.addEast(sideExpressionTable, 550);
    splitLayout.add(expressionTable);    
    return splitLayout;
  }
  
  @Override
  protected void addToolbars() {
    super.addToolbars();
    Button flipButton = new Button("Flip mRNA-microRNA", 
      (ClickHandler) event -> flipDualView());
    Widget tools = Utils.mkHorizontalPanel(true, flipButton);
    mainTools.add(tools);
  }
  
  @Override
  protected void setupMenuItems() {
    super.setupMenuItems();
    MenuItem mi = new MenuItem("Download interaction network (DOT)...", 
      () -> downloadNetwork(Format.DOT));             
    addAnalysisMenuItem(mi);
    
    mi = new MenuItem("Download interaction network (SIF)...", 
      () -> downloadNetwork(Format.SIF));       
    addAnalysisMenuItem(mi);
    
    mi = new MenuItem("Download interaction network (Custom)...", 
      () -> downloadNetwork(Format.Custom));       
    addAnalysisMenuItem(mi);    
  }
  
  protected void downloadNetwork(Format format) {
    if (mode.isSplit) {
      MatrixServiceAsync matrixService = manager().matrixService();
      Network network = controller.buildNetwork("miRNA-mRNA interactions", mode != DualMode.Forward);
      matrixService.prepareNetworkDownload(network, format, new PendingAsyncCallback<String>(this) {
        public void handleSuccess(String url) {
          Utils.displayURL("Your download is ready.", "Download", url);
        }
      });
    } else {
      Window.alert("Please view mRNA and miRNA samples simultaneously to download networks.");
    }
  }
  
  protected Set<String> getIndicatedRows(@Nullable String selected, boolean fromMain) {
    Map<String, Collection<String>> lookup = fromMain ? linkingMap() : mappingSummary.getReverseMap();    
    if (selected != null) {   
      if (lookup != null && lookup.containsKey(selected)) {          
        return new HashSet<String>(lookup.get(selected));        
      } else {
        logger.warning("No association indications for " + selected);
      }
    }                
    return new HashSet<String>();
  }
  
  //Initial title only - need the constant here since the field won't be initialised
  @Override
  protected String mainTableTitle() { return "mRNA"; }     
  
  protected boolean mainTableSelectable() { return true; }  
  
  protected List<Group> columnsOfType(List<Group> from, String type) {
    return from.stream().filter(g -> type.equals(GroupUtils.groupType(g))).
        collect(Collectors.toList());    
  }
  
  protected List<Group> columnsForMainTable(List<Group> from) {    
    List<Group> r = columnsOfType(from, mode.mainType);
    
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<Group> columnsForSideTable(List<Group> from) {
    return columnsOfType(from, mode.sideType);        
  }
  
  protected TableStyle mainTableStyle() {
    return super.styleForColumns(columnsForMainTable(chosenColumns));
  }

  protected TableStyle sideTableStyle() {
    return super.styleForColumns(columnsForSideTable(chosenColumns));    
  }
  
  /**
   * Based on the available columns, pick the correct display mode.
   * The mode may be split or non-split.
   */
  protected DualMode pickMode(List<Group> columns) {
    String[] types = columns.stream().map(g -> GroupUtils.groupType(g)).distinct().
        toArray(String[]::new);
    if (types.length >= 2) {
      return preferredDoubleMode;
    } else if (types.length == 1 && types[0].equals("mRNA")) {
      return DualMode.SingleMRNA;
    } else if (types.length == 1 && types[0].equals("miRNA")) {
      return DualMode.SingleMiRNA;
    } else {
      logger.warning("No valid dual mode found.");
      return DualMode.SingleMRNA;
    }
  }
  
  @Override
  public void loadState(StorageParser p, DataSchema schema, AttributeSet attributes) {
    //TODO this is a state management hack to force the columns to be fully re-initialised
    //every time we show the screen.
    chosenColumns = new ArrayList<Group>();
    super.loadState(p, schema, attributes);
  }
  
  @Override
  protected void changeColumns(List<Group> columns) {
    logger.info("Dual mode pick for " + columns.size() + " columns");
    mode = pickMode(columns);    
    
    expressionTable.setTitleHeader(mode.mainType);    
    expressionTable.setStyleAndApply(mode.mainStyle());
    if (mode.isSplit) {
      sideExpressionTable.setTitleHeader(mode.sideType);      
      sideExpressionTable.setStyleAndApply(mode.sideStyle());
    }

    super.changeColumns(columnsForMainTable(columns));
    
    List<Group> sideColumns = columnsForSideTable(columns);
    if (sideExpressionTable != null && !sideColumns.isEmpty()) {    
      sideExpressionTable.columnsChanged(sideColumns);    
    }
    logger.info("Dual table mode: " + mode);

    splitLayout.setWidgetSize(sideExpressionTable, mode.sideTableWidth());
  }  
  
  @Override
  public void updateProbes() {   
    if (mode.isSplit) {
      expressionTable.setAssociationAutoRefresh(false);
      mode.setVisibleColumns(expressionTable);      
      expressionTable.setAssociationAutoRefresh(true);
    }    
    
    super.updateProbes();
    sideExpressionTable.clearMatrix();
    sideExpressionTable.setIndicatedProbes(new HashSet<String>(), false);
    expressionTable.setIndicatedProbes(new HashSet<String>(), false);    
  }
  
  @Override
  protected void associationsUpdated(Association[] result) {
    super.associationsUpdated(result);    
    if (mode.isSplit) {
      extractSideTableProbes();
    }
  }
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    if (mode.isSplit) {
      sideExpressionTable.clearMatrix();
    }
  }
  
  //Maps main table to side table via a column.
  protected AssociationSummary<ExpressionRow> mappingSummary;
  
  private Synthetic.Precomputed buildCountColumn(String[][] rawData) {
    Map<String, Double> counts = new HashMap<String, Double>();    
    //The first row is headers    
    for (int i = 1; i < rawData.length && i < MAX_SECONDARY_ROWS; i++) {    
      counts.put(rawData[i][1], Double.parseDouble(rawData[i][2]));
    }
    return new Synthetic.Precomputed("Count", 
      "Number of times each " + mode.sideType + " appeared", counts,
      null);

  }
  
  protected void extractSideTableProbes() {
    mappingSummary = expressionTable.associationSummary(mode.linkingType);  
    if (sideExpressionTable.chosenColumns().isEmpty()) {
      return;
    }
    
    if (mappingSummary == null) {
      logger.info("Unable to get miRNA-mRNA summary - not updating side table probes");
      return;
    }
    String[][] rawData = mappingSummary.getTable();
    if (rawData.length < 2) {
      logger.info("No secondary probes found in summary - not updating side table probes");
      return;
    }
    String[] ids = Arrays.stream(rawData).skip(1).limit(MAX_SECONDARY_ROWS).
        map(a -> a[1]).toArray(String[]::new);
    logger.info("Extracted " + ids.length + " " + mode.sideType);    
    
    Synthetic.Precomputed countColumn = buildCountColumn(rawData);
    List<Synthetic> synths = Arrays.asList(countColumn);
    
    changeSideTableProbes(ids, synths);
  }
  
  protected void changeSideTableProbes(String[] probes, List<Synthetic> synths) {
    sideExpressionTable.probesChanged(probes);
    if (probes.length > 0) {
      sideExpressionTable.getExpressions(synths, true);
    }
  }
  
  /**
   * Build Nodes by using expression values from the first column in the rows.
   * @param type
   * @param rows
   * @return
   */
  static List<Node> buildNodes(String kind, List<ExpressionRow> rows) {
    return rows.stream().map(r -> 
      Node.fromRow(r, kind)).collect(Collectors.toList());    
  }
  
  /**
   * Maps mRNA-miRNA in forward mode, miRNA-mRNA in reverse mode
   * @return
   */
  protected Map<String, Collection<String>> linkingMap() {    
    return mappingSummary.getFullMap();
  }
  
  @Override
  public List<Node> getSourceNodes() {
    return buildNodes(mode.mainType, expressionTable.getDisplayedRows());
  }
  
  @Override
  public List<Node> getDestNodes() {
    return buildNodes(mode.sideType, sideExpressionTable.getDisplayedRows());    
  }
  
  @Override
  public String getSourceType() {
    return mode.mainType;
  }
  
  @Override
  public String getDestType() {
    return mode.sideType;
  }
  
  @Nullable 
  private String getSelectedNode(ExpressionTable table) {
    ExpressionRow r =
        ((SingleSelectionModel<ExpressionRow>) table.selectionModel()).getSelectedObject();
    return (r != null ? r.getProbe() : null);
  }
  
  @Override
  public @Nullable String getSelectedSourceNode() {
    return getSelectedNode(expressionTable);    
  }
  
  @Override
  public @Nullable String getSelectedDestNode() {
    return getSelectedNode(sideExpressionTable);    
  }

  @Override
  public void setHighlightedSourceNodes(Set<String> selected) {
    expressionTable.setIndicatedProbes(selected, true);
  }
  
  @Override
  public void setHighlightedDestNodes(Set<String> selected) {
    sideExpressionTable.setIndicatedProbes(selected, true);
  }

  @Override
  public void onSourceSelectionChanged(String node) {    
    if (mode.isSplit) {
      setHighlightedDestNodes(getIndicatedRows(getSelectedSourceNode(), true));
    }    
  }

  @Override
  public void onDestSelectionChanged(String node) {
    if (mode.isSplit) {
      setHighlightedSourceNodes(getIndicatedRows(getSelectedDestNode(), false));
    }
  }
}

