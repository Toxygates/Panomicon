package otgviewer.client;

import java.util.*;
import java.util.stream.Collectors;


import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.ScreenManager;
import t.common.shared.AType;
import t.common.shared.GroupUtils;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.Utils;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.table.*;
import t.viewer.shared.*;
import t.viewer.shared.network.*;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * A DataScreen that can display two tables side by side.
 * The dual mode will only activate if appropriate columns have been
 * defined and saved for both tables. Otherwise, the screen will revert to
 * single-table mode.
 * 
 * The "main" table drives the side table, in the sense that what is being displayed in the
 * latter depends on the content of the former.
 */
public class DualDataScreen extends DataScreen {

  protected ExpressionTable sideExpressionTable;
  
  protected final static String sideMatrix = "SECONDARY";
  
  protected String mainTableType = "mRNA";
  protected String sideTableType = "miRNA";
  protected boolean reverseMode = false;
  
  public DualDataScreen(ScreenManager man) {
    super(man);
    
    final int MAX_SECONDARY_ROWS = 1000;
    
    TableFlags flags = new TableFlags(sideMatrix, true, false, 
        MAX_SECONDARY_ROWS, sideTableType, true, true);
    sideExpressionTable = new ExpressionTable(this, flags,
        TableStyle.getStyle(sideTableType));     
    sideExpressionTable.addStyleName("sideExpressionTable");
    
    expressionTable.selectionModel().addSelectionChangeHandler(e ->
      setIndications(expressionTable, sideExpressionTable, true));      
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e ->
      setIndications(sideExpressionTable, expressionTable, false));
  }
  
  protected void setDualView(String mainType, String sideType) {
    mainTableType = mainType;
    sideTableType = sideType;
    expressionTable.setTitleHeader(mainTableType);
    sideExpressionTable.setTitleHeader(sideTableType);
  }
  
  protected void flipDualView() {
    reverseMode = !reverseMode;
    setDualView(sideTableType, mainTableType);    
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
    MatrixServiceAsync matrixService = manager().matrixService();
    Network network = buildNetwork("miRNA-mRNA interactions");
    matrixService.prepareNetworkDownload(network, format, new PendingAsyncCallback<String>(this) {
      public void handleSuccess(String url) {
        Utils.displayURL("Your download is ready.", "Download", url);
      }
    });
  }
  
  /**
   * When the selection in one of the tables has changed, highlight the associated rows
   * in the other table.
   * @param fromTable
   * @param toTable
   * @param fromType
   */
  protected void setIndications(ExpressionTable fromTable, ExpressionTable toTable,
                                boolean fromMain) {
    ExpressionRow r = ((SingleSelectionModel<ExpressionRow>) fromTable.selectionModel()).
        getSelectedObject();    
    toTable.setIndicatedProbes(getIndicatedRows(r.getProbe(), fromMain));
  }
  
  protected String[] getIndicatedRows(String selected, boolean fromMain) {
    Map<String, Collection<AssociationValue>> lookup = linkingMap();
    if (fromMain) {   
      Collection<AssociationValue> assocs = lookup.get(selected);
      if (assocs != null) {
        String[] results =
            lookup.get(selected).stream().map(av -> av.formalIdentifier()).toArray(String[]::new);
        return results;
      } else {
        logger.warning("No association indications for " + selected);
      }
    } else {                 
      String[] results = lookup.keySet().stream().filter(er -> 
        lookup.get(er).stream().map(av -> av.formalIdentifier()).
        anyMatch(id -> id.equals(selected))).toArray(String[]::new);
      return results;      
    }
    return new String[0];
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
    List<Group> r = columnsOfType(from, mainTableType);
    
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<Group> columnsForSideTable(List<Group> from) {
    return columnsOfType(from, sideTableType);        
  }
  
  protected TableStyle mainTableStyle() {
    return super.styleForColumns(columnsForMainTable(chosenColumns));
  }

  protected TableStyle sideTableStyle() {
    return super.styleForColumns(columnsForSideTable(chosenColumns));    
  }
  
  @Override
  protected void changeColumns(List<Group> columns) {    
    super.changeColumns(columnsForMainTable(columns));    
    List<Group> sideColumns = columnsForSideTable(columns);
    if (sideExpressionTable != null && !sideColumns.isEmpty()) {
      sideExpressionTable.columnsChanged(sideColumns);
    }  
    if (!sideColumns.isEmpty()) {
      splitLayout.setWidgetSize(sideExpressionTable, 550);
    } else {
      splitLayout.setWidgetSize(sideExpressionTable, 0);
    }
  }  
  
  @Override
  public void updateProbes() {
    super.updateProbes();
    sideExpressionTable.clearMatrix();
    extractSideTableProbes();
  }
  
  @Override
  protected void associationsUpdated(Association[] result) {
    super.associationsUpdated(result);    
    extractSideTableProbes();
  }
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    sideExpressionTable.clearMatrix();
  }
  
  protected AType typeForSideTable() {
    if (reverseMode) {
      return AType.MRNA;
    } 
    return AType.MiRNA;
  }
  
  protected AssociationSummary<ExpressionRow> mappingSummary; 
  protected void extractSideTableProbes() {
    mappingSummary = expressionTable.associationSummary(typeForSideTable());
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
    String[] ids = new String[rawData.length - 1];
    Map<String, Double> counts = new HashMap<String, Double>();
    
    //The first row is headers
    for (int i = 1; i < rawData.length; i++) {    
      ids[i - 1] = rawData[i][1];
      counts.put(rawData[i][1], Double.parseDouble(rawData[i][2]));
    }
    
    logger.info("Extracted " + ids.length + " " + sideTableType);    
    
    Synthetic.Precomputed countColumn = new Synthetic.Precomputed("Count", 
      "Number of times each " + sideTableType + " appeared", counts,
      null);

    List<Synthetic> synths = new ArrayList<Synthetic>();
    synths.add(countColumn);
    
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
  protected Map<String, Collection<AssociationValue>> linkingMap() {    
    return mappingSummary.getFullMap();
  }
  
  /**
   * Build the interaction network represented by the current view in the two tables.
   * @return
   */
  public Network buildNetwork(String title) {
    Map<String, ExpressionRow> lookup = new HashMap<String, ExpressionRow>();
    List<Node> nodes = new ArrayList<Node>();
    nodes.addAll(buildNodes(mainTableType, expressionTable.getDisplayedRows()));    
    nodes.addAll(buildNodes(sideTableType, sideExpressionTable.getDisplayedRows()));
        
    expressionTable.getDisplayedRows().stream().forEach(r -> lookup.put(r.getProbe(), r));
    sideExpressionTable.getDisplayedRows().stream().forEach(r -> lookup.put(r.getProbe(), r));
    
    List<Interaction> interactions = new ArrayList<Interaction>();
    Map<String, Collection<AssociationValue>> fullMap = linkingMap();
    for (String mainProbe: fullMap.keySet()) {
      for (AssociationValue av: fullMap.get(mainProbe)) {
        Node side = Node.fromAssociation(av, sideTableType);
        Node main = Node.fromRow(lookup.get(mainProbe), mainTableType);
        
        //Directed interaction normally from miRNA to mRNA
        Node from = reverseMode ? main : side;
        Node to = reverseMode ? side: main;
        Interaction i = new Interaction(from, to, null, null);
        interactions.add(i);
      }
    }
    return new Network(title, nodes, interactions);
  }
}

