package t.viewer.client.components;

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.viewer.shared.Association;

/**
 * A composite that displays information from a set of columns and a 
 * set of probes. 
 */
public abstract class DataView extends Composite {

  protected String[] lastProbes;
  protected List<Group> lastColumns;
  
  protected SampleClass chosenSampleClass;
  protected String[] chosenProbes = new String[0];
  protected List<Group> chosenColumns = new ArrayList<Group>();
  
  public DataView() {
    fileMenu = addMenu("File");
    analysisMenu = addMenu("Tools");    
  }
  
  private MenuBar addMenu(String title) {
    MenuBar mb = new MenuBar(true);
    MenuItem mi = new MenuItem(title, false, mb);
    topLevelMenus.add(mi);
    return mb;
  }
  
  /**
   * May be overridden to display status messages about data loading
   * @param message
   */
  protected void displayInfo(String message) {}
  
  //  protected void beforeGetAssociations() {}
  
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
  
  protected MenuBar analysisMenu, fileMenu;
  
  public MenuBar analysisMenu() { return analysisMenu; }
  public MenuBar fileMenu() { return fileMenu; }

  protected List<MenuItem> topLevelMenus = new ArrayList<MenuItem>();
  
  protected void addAnalysisMenuItem(MenuItem mi) {
    analysisMenu.addItem(mi);
  }
  
  protected void addTopLevelMenu(MenuItem mi) {
    topLevelMenus.add(mi);
  }
 
  /**
   * Top level menus to be installed.
   */
  public Collection<MenuItem> topLevelMenus() { return topLevelMenus; }
  
  abstract public String[] displayedAtomicProbes();
  
  @Nullable 
  public Widget tools() { return null; }
  
  
  protected List<Widget> toolbars = new ArrayList<Widget>();
  protected void addToolbar(Widget toolbar) {
    toolbars.add(toolbar);
  }
  
  public Collection<Widget> toolbars() { return toolbars; }
  
  public void columnsChanged(List<Group> columns) {
    chosenColumns = columns;
  }

  public List<Group> chosenColumns() {
    return chosenColumns;
  }
  
  public void sampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;
  }

  public void probesChanged(String[] probes) {
    chosenProbes = probes;
  }
}
