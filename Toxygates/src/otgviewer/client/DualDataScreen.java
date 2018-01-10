package otgviewer.client;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.ScreenManager;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.Utils;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.table.*;
import t.viewer.shared.*;
import t.viewer.shared.network.*;

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
  
  protected final static String mainTableType = "mRNA";
  protected final static String sideTableType = "miRNA";
  
  public DualDataScreen(ScreenManager man) {
    super(man);
    
    final int MAX_SECONDARY_ROWS = 1000;
    
    TableFlags flags = new TableFlags(sideMatrix, true, false, 
        MAX_SECONDARY_ROWS, sideTableType, true, true);
    sideExpressionTable = new ExpressionTable(this, flags,
        TableStyle.getStyle(sideTableType));     
    sideExpressionTable.addStyleName("sideExpressionTable");
    
    expressionTable.selectionModel().addSelectionChangeHandler(e ->
      mainTableSelectionChange());
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e ->
      sideTableSelectionChange());
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
    
    mi = new MenuItem("Swap mRNA-miRNA tables", () -> swapTables());
  }
  
  protected void swapTables() {
    //TODO
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
  
  protected void mainTableSelectionChange() {
    ExpressionRow r = ((SingleSelectionModel<ExpressionRow>) expressionTable.selectionModel()).
        getSelectedObject();
    Map<ExpressionRow, Collection<AssociationValue>> lookup = mrnaToMirnaMap();
    String[] mirnas = lookup.get(r).stream().map(av -> av.formalIdentifier()).toArray(String[]::new);
    sideExpressionTable.setIndicatedProbes(mirnas);    
  }
  
  protected void sideTableSelectionChange() {
    ExpressionRow r = ((SingleSelectionModel<ExpressionRow>) sideExpressionTable.selectionModel()).
        getSelectedObject();
    String mirna = r.getProbe();
    Map<ExpressionRow, Collection<AssociationValue>> lookup = mrnaToMirnaMap();
    String[] mrnas = lookup.keySet().stream().filter(er -> 
      lookup.get(er).stream().map(av -> av.formalIdentifier()).anyMatch(id -> id.equals(mirna))).
      map(er -> er.getProbe()).
      toArray(String[]::new);
    expressionTable.setIndicatedProbes(mrnas);
      
  }
  
  @Override
  protected String mainTableTitle() { return mainTableType; }     
  
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
    extractMirnaProbes();
  }
  
  @Override
  protected void associationsUpdated() {
    super.associationsUpdated();
    extractMirnaProbes();
  }
  
  private ColumnFilter lastCountFilter = null;
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    lastCountFilter = countColumnFilter();
    sideExpressionTable.clearMatrix();
  }
  
  protected AssociationSummary<ExpressionRow> mirnaSummary; 
  protected void extractMirnaProbes() {
    mirnaSummary = expressionTable.associationSummary(AType.MiRNA);
    if (sideExpressionTable.chosenColumns().isEmpty()) {
      return;
    }
    
    if (mirnaSummary == null) {
      logger.info("Unable to get miRNA summary - not updating side table probes");
      return;
    }
    String[][] rawData = mirnaSummary.getTable();
    if (rawData.length < 2) {
      logger.info("No miRNAs found in summary - not updating side table probes");
      return;
    }
    String[] ids = new String[rawData.length - 1];
    Map<String, Double> counts = new HashMap<String, Double>();
    
    //The first row is headers
    for (int i = 1; i < rawData.length; i++) {    
      ids[i - 1] = rawData[i][1];
      counts.put(rawData[i][1], Double.parseDouble(rawData[i][2]));
    }
    
    logger.info("Extracted " + ids.length + " miRNAs");    
    
    Synthetic.Precomputed countColumn = new Synthetic.Precomputed("Count", 
      "Number of times each miRNA appeared", counts,
      lastCountFilter);

    List<Synthetic> synths = new ArrayList<Synthetic>();
    synths.add(countColumn);
    
    changeSideTableProbes(ids, synths);
  }
  
  private @Nullable ColumnFilter countColumnFilter() {
    ManagedMatrixInfo matInfo = sideExpressionTable.currentMatrixInfo();
    if (matInfo == null) {
      return null;
    }
    int numData = matInfo.numDataColumns();
    int numSynth = matInfo.numSynthetics();
    if (numData > 0 && numSynth > 0) {
      //We rely on the count column being the first synthetic
      return matInfo.columnFilter(numData);
    } else {
      return null;
    }
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
  
  protected Map<ExpressionRow, Collection<AssociationValue>> mrnaToMirnaMap() {    
    return mirnaSummary.getFullMap();
  }
  
  /**
   * Build the interaction network represented by the current view in the two tables.
   * @return
   */
  public Network buildNetwork(String title) {
    List<Node> nodes = new ArrayList<Node>();
    nodes.addAll(buildNodes(mainTableType, expressionTable.getDisplayedRows()));    
    nodes.addAll(buildNodes(sideTableType, sideExpressionTable.getDisplayedRows()));
        
    List<Interaction> interactions = new ArrayList<Interaction>();
    Map<ExpressionRow, Collection<AssociationValue>> fullMap = mrnaToMirnaMap();
    for (ExpressionRow row: fullMap.keySet()) {
      for (AssociationValue av: fullMap.get(row)) {
        Node from = Node.fromAssociation(av, sideTableType);
        Node to = Node.fromRow(row, mainTableType);         
        Interaction i = new Interaction(from, to, null, null);
        interactions.add(i);
      }
    }
    return new Network(title, nodes, interactions);
  }
}

