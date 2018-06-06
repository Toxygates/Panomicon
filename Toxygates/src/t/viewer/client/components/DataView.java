package t.viewer.client.components;

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.DataListenerWidget;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.viewer.client.PersistedState;
import t.viewer.client.table.ExpressionTable;
import t.viewer.shared.Association;

/**
 * A composite that displays information from a set of columns and a 
 * set of probes. 
 */
public abstract class DataView extends DataListenerWidget {

  protected String[] lastProbes;
  protected List<Group> lastColumns;
  
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
  
  abstract public ValueType chosenValueType();
  
  /**
   * Reload data if necessary, when probes or columns have changed
   */
  abstract public void reloadDataIfNeeded();
  
  //TODO remove
  abstract public ExpressionTable expressionTable();
  
  protected List<MenuItem> analysisMenuItems = new ArrayList<MenuItem>();
  protected List<MenuItem> topLevelMenus = new ArrayList<MenuItem>();
  
  protected void addAnalysisMenuItem(MenuItem mi) {
    analysisMenuItems.add(mi);
  }
  
  protected void addTopLevelMenu(MenuItem mi) {
    topLevelMenus.add(mi);
  }
  
  /**
   * Menu items to be added to the analysis menu.
   */
  public Collection<MenuItem> analysisMenuItems() { return analysisMenuItems; }
 
  /**
   * Top level menus to be installed.
   */
  public Collection<MenuItem> topLevelMenus() { return topLevelMenus; }
 
  public List<PersistedState<?>> getPersistedItems() {
    return new ArrayList<PersistedState<?>>();
  }
  
  public void loadPersistedState() { }
  
  abstract public String[] displayedAtomicProbes();
  
  @Nullable 
  public Widget tools() { return null; }
  
  
  protected List<Widget> toolbars = new ArrayList<Widget>();
  protected void addToolbar(Widget toolbar) {
    toolbars.add(toolbar);
  }
  
  public Collection<Widget> toolbars() { return toolbars; }
}
