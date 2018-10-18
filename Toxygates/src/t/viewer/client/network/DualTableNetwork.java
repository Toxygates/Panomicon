package t.viewer.client.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.view.client.SingleSelectionModel;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.table.AssociationSummary;
import t.viewer.client.table.DualTableView.DualMode;
import t.viewer.client.table.ExpressionTable;

public class DualTableNetwork implements NetworkViewer {
  private final ExpressionTable mainTable, sideTable;
  
  private final DualMode dualMode;
  
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
   * To be called each time the main table rows have changed.
   */
  public void updateLinkingMap() {
    mappingSummary = mainTable.associations().associationSummary(dualMode.linkingType);
    if (sideTable.chosenColumns().isEmpty()) {
      return;
    }
    
    if (mappingSummary == null) {
      logger.info("Unable to get miRNA-mRNA summary - not updating side table probes");     
    }          
  }
}
