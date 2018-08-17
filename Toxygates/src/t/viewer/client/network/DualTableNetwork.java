package t.viewer.client.network;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.table.*;
import t.viewer.client.table.DualTableView.DualMode;
import t.viewer.shared.ColumnSet;
import t.viewer.shared.network.Network;
import t.viewer.shared.network.Node;

import com.google.gwt.view.client.SingleSelectionModel;

public class DualTableNetwork implements NetworkViewer {
  private final ExpressionTable mainTable, sideTable;
  
  private final DualMode dualMode;
  
  private Logger logger = SharedUtils.getLogger("dualTableNetwork");
  
  private @Nullable Network network;
  
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
  }
  
  public void setNetwork(Network network) {
    this.network = network;
  }
  
  @Override
  public List<Node> getSourceNodes() {
    String type = (dualMode == DualMode.Forward) ? Network.mrnaType : Network.mirnaType; 
    return buildNodes(type, mainTable.getDisplayedRows(), mainTable.matrixInfo);
  }
  
  @Override
  public List<Node> getDestNodes() {
    String type = (dualMode == DualMode.Forward) ? Network.mirnaType : Network.mrnaType;
    return buildNodes(type, sideTable.getDisplayedRows(), sideTable.matrixInfo);
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
  
  protected Set<String> getIndicatedRows(@Nullable String selected, 
      boolean selectionFromMainTable) {    
    if (selected != null) {
      if ((dualMode == DualMode.Forward && !selectionFromMainTable) || 
          (dualMode == DualMode.Reverse && selectionFromMainTable)) {
        return new HashSet<String>(interactionsFrom(selected));
      } else {
        return new HashSet<String>(interactionsTo(selected));
      }
    } else {
      return new HashSet<String>();
    }
  }

  protected List<String> interactionsTo(String node) {
    if (network == null) {
      return new ArrayList<String>();
    }
    List r = network.interactionsTo(node).stream().map(x -> x.from().id()).collect(Collectors.toList());
    logger.info("Interactions to " + node + ": " + r.size());
    return r;
  }
  
  protected List<String> interactionsFrom(String node) {
    if (network == null) {
      return new ArrayList<String>();
    }
    return network.interactionsFrom(node).stream().map(x -> x.to().id()).collect(Collectors.toList());
  }
  
  
  /**
   * Build Nodes by using expression values from the first column in the rows.
   * @param type
   * @param rows
   * @return
   */
  static List<Node> buildNodes(String kind, List<ExpressionRow> rows, ColumnSet columnNames) {
    return rows.stream().map(r -> Node.fromRow(r, kind, columnNames)).collect(Collectors.toList());
  }
}
