package t.viewer.client.network;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.view.client.SingleSelectionModel;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.table.AssociationSummary;
import t.viewer.client.table.DualTableView.DualMode;
import t.viewer.client.table.ExpressionTable;
import t.viewer.shared.Synthetic;
import t.viewer.shared.network.Network;
import t.viewer.shared.network.Node;

public class DualTableNetwork implements NetworkViewer {
  private final ExpressionTable mainTable, sideTable;
  
  private final DualMode dualMode;
  
  private final int maxSideRows;
  
  private Logger logger = SharedUtils.getLogger("dualTableNetwork");
  
  /**
   * An interaction network displayed in two ExpressionTables.
   * 
   * @param mainTable The table that shows the main nodes (controlling the network display)
   * @param sideTable The table that shows the secondary nodes.
   * @param dualMode The mode of the network (encodes information about the types of nodes in each
   *        table, and how they are linked
   */
  public DualTableNetwork(ExpressionTable mainTable, ExpressionTable sideTable,                          
      DualMode dualMode, int maxSideRows) {
    this.mainTable = mainTable;
    this.sideTable = sideTable;
    this.dualMode = dualMode;
    this.maxSideRows = maxSideRows;
  }
  
  @Override
  public List<Node> getSourceNodes() {
    String type = (dualMode == DualMode.Forward) ? Network.mrnaType : Network.mirnaType; 
    return buildNodes(type, mainTable.getDisplayedRows(), index -> mainTable.matrixInfo.columnName((index)));
  }
  
  @Override
  public List<Node> getDestNodes() {
    String type = (dualMode == DualMode.Forward) ? Network.mirnaType : Network.mrnaType;
    return buildNodes(type, sideTable.getDisplayedRows(), index -> sideTable.matrixInfo.columnName((index)));
  }
  
  @Nullable 
  private String getSelectedNode(ExpressionTable table) {
    ExpressionRow r =
        ((SingleSelectionModel<ExpressionRow>) table.selectionModel()).getSelectedObject();
    return (r != null ? r.getProbe() : null);
  }
  
  @Override
  public @Nullable String getSelectedSourceNode() {
    return getSelectedNode(mainTable);    
  }
  
  @Override
  public @Nullable String getSelectedDestNode() {
    return getSelectedNode(sideTable);    
  }

  @Override
  public void setHighlightedSourceNodes(Set<String> selected) {
    mainTable.setIndicatedProbes(selected, true);
  }
  
  @Override
  public void setHighlightedDestNodes(Set<String> selected) {
    sideTable.setIndicatedProbes(selected, true);
  }

  @Override
  public void onSourceSelectionChanged() {      
    setHighlightedDestNodes(getIndicatedRows(getSelectedSourceNode(), true));
  }

  @Override
  public void onDestSelectionChanged() {    
    setHighlightedSourceNodes(getIndicatedRows(getSelectedDestNode(), false));    
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
  
//Maps main table to side table via a column.
  protected AssociationSummary<ExpressionRow> mappingSummary;
  
  /**
   * Maps mRNA-miRNA in forward mode, miRNA-mRNA in reverse mode
   * @return
   */
  public Map<String, Collection<String>> linkingMap() {    
    return mappingSummary.getFullMap();
  }

  /**
   * To be called each time the main table rows have changed, thus triggering updates.
   * Future: this class could install a listener in the mainTable by itself?
   */
  public void extractSideTableProbes() {
    mappingSummary = mainTable.associationSummary(dualMode.linkingType);
    if (sideTable.chosenColumns().isEmpty()) {
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
    String[] ids = Arrays.stream(rawData).skip(1).limit(maxSideRows).
        map(a -> a[1]).toArray(String[]::new);
    logger.info("Extracted " + ids.length + " " + dualMode.sideType);
    
    Synthetic.Precomputed countColumn = buildCountColumn(rawData);
    List<Synthetic> synths = Arrays.asList(countColumn);
    
    changeSideTableProbes(ids, synths);
  }
  
  protected void changeSideTableProbes(String[] probes, List<Synthetic> synths) {
    sideTable.probesChanged(probes);
    if (probes.length > 0) {
      sideTable.getExpressions(synths, true);
    }
  }
  
  private Synthetic.Precomputed buildCountColumn(String[][] rawData) {
    Map<String, Double> counts = new HashMap<String, Double>();    
    //The first row is headers    
    for (int i = 1; i < rawData.length && i < maxSideRows; i++) {    
      counts.put(rawData[i][1], Double.parseDouble(rawData[i][2]));
    }
    return new Synthetic.Precomputed("Count", 
      "Number of times each " + dualMode.sideType + " appeared", counts,
      null);

  }
  
  /**
   * Build Nodes by using expression values from the first column in the rows.
   * @param type
   * @param rows
   * @return
   */
  static List<Node> buildNodes(String kind, List<ExpressionRow> rows, Node.ColumnNameProvider columnNames) {
    return rows.stream().map(r -> Node.fromRow(r, kind, columnNames)).collect(Collectors.toList());
  }
}
