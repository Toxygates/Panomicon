package t.viewer.client.components;

import java.util.*;

import otgviewer.client.components.DataListenerWidget;
import t.common.shared.sample.Group;
import t.viewer.client.table.ExpressionTable;
import t.viewer.shared.Association;

/**
 * A composite that displays information from a set of columns and a 
 * set of probes. 
 */
public abstract class DataView extends DataListenerWidget {

  protected String[] lastProbes;
  protected List<Group> lastColumns;
  //menu items
  
  //toolbars
  
  //associations
  
  /**
   * May be overridden to display status messages about data loading
   * @param message
   */
  protected void displayInfo(String message) {}
  
  protected void beforeGetAssociations() {}
  
  protected void associationsUpdated(Association[] result) {
    Optional<Association> overLimit = 
        Arrays.stream(result).filter(a -> a.overSizeLimit()).findAny();
    if (overLimit.isPresent()) {       
      displayInfo("Too many associations, limited view.");
    } else {
      displayInfo("");
    }    
  }
  
  /**
   * Reload data if necessary, when probes or columns have changed
   */
  abstract public void reloadDataIfNeeded();
  
  //TODO remove
  abstract public ExpressionTable expressionTable();
}
