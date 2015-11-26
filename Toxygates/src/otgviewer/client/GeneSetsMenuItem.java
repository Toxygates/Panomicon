package otgviewer.client;

import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;

import otgviewer.client.components.DataListenerWidget;
import t.common.shared.ClusteringList;
import t.common.shared.ItemList;
import t.common.shared.SharedUtils;
import t.common.shared.StringList;
import t.common.shared.userclustering.Algorithm;

public class GeneSetsMenuItem extends DataListenerWidget {

  protected final Logger logger = SharedUtils.getLogger("GeneSetsMenuItem");

  private final DataScreen screen;

  private MenuBar root;
  private MenuItem mi;

  public GeneSetsMenuItem(DataScreen screen) {
    this.screen = screen;
    root = new MenuBar(true);
    mi = new MenuItem("Gene Sets", false, root);

    createMenuItem();
  }

  protected boolean hasUserClustering() {
    return true;
  }

  protected boolean hasPredefinedClustering() {
    return true;
  }

  /*
   * Hack GWT-MenuItemSeparator to indicate caption upon the separator
   */
  class MenuItemCaptionSeparator extends MenuItemSeparator {
    public MenuItemCaptionSeparator(String caption) {
      super();
      getElement().removeAllChildren();
      
      Element div = DOM.createDiv();
      div.setInnerText(caption);
      DOM.appendChild(getElement(), div);
      setStyleName(div, "menuSeparatorCaption");

      div = DOM.createDiv();
      DOM.appendChild(getElement(), div);
      setStyleName(div, "menuSeparatorInner");
    }
  }

  private void createMenuItem() {
    createUserSets();
    
    if (hasUserClustering()) {
      createUserClusterings();
    }

    if (hasPredefinedClustering()) {
    }

  }
  
  private void createUserSets() {
    root.addSeparator(new MenuItemCaptionSeparator("User sets"));
    
    List<StringList> geneSets = StringList.pickProbeLists(screen.chosenItemLists, null);
    for (final StringList sl : geneSets) {
      MenuItem item = new MenuItem(sl.name(), false, new Command() {
        public void execute() {
          screen.probesChanged(sl.items());
          screen.geneSetChanged(sl.name());
          screen.updateProbes();
        }
      });
      root.addItem(item);
    }

    root.addSeparator(new MenuItemSeparator());
    root.addItem(new MenuItem("Add new", false, addNewUserSet()));
    root.addItem(new MenuItem("Manage", false, manageUserSet()));
  }
  
  private Command addNewUserSet() {
    return new Command() {
      public void execute() {
        
      }
    };
  }

  private Command manageUserSet() {
    return new Command() {
      public void execute() {
        
      }
    };
  }

  private void createUserClusterings() {
    root.addSeparator(new MenuItemCaptionSeparator("Clusterings (user)"));
    
    List<ClusteringList> clusterings = ClusteringList.pickUserClusteringLists(screen.chosenClusteringList, null);
    logger.info("Obtained " + clusterings.size() +  " user clusterings.");
    for (final ClusteringList cl : clusterings) {
      MenuBar mb = new MenuBar(true);
      
      String caption = clusteringCaption(cl.algorithm());
      mb.addSeparator(new MenuItemCaptionSeparator(caption));
      
      for (final StringList sl : cl.items()) {
        MenuItem item = new MenuItem(sl.name(), false, new Command() {
          public void execute() {
            screen.probesChanged(sl.items());
            screen.geneSetChanged(sl.name());
            screen.updateProbes();
          }
        });
        mb.addItem(item);
      }
      root.addItem(cl.name(), mb);
    }

    root.addSeparator(new MenuItemSeparator());
    root.addItem(new MenuItem("Add new", false, addNewUserSet()));
    root.addItem(new MenuItem("Manage", false, manageUserSet()));
  }
  
  private String clusteringCaption(Algorithm algorithm) {
    StringBuffer sb = new StringBuffer();
    sb.append("Row -> ");
    sb.append(algorithm.getRowMethod().asParam());
    sb.append(", ");
    sb.append(algorithm.getRowDistance().asParam());
    sb.append("Col -> ");
    sb.append(algorithm.getColMethod().asParam());
    sb.append(", ");
    sb.append(algorithm.getColDistance().asParam());
    return sb.toString();
  }

  public MenuItem menuItem() {
    return mi;
  }

  /**
   *  Refresh menu items on itemListsChanged fired.
   *  Note the events would be also fired when the DataScreen is activated.
   *  [DataScreen#show -> Screen#show -> Screen#lodaState -> DataListenerWidget#lodaState]
   *  
   *  @see otgviewer.client.DataScreen#show()
   */
  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    root.clearItems();
    createMenuItem();
  }

}
