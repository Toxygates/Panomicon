/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

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
      div.setInnerHTML(caption.replaceAll("\n", "<br>"));
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
    root.addItem(new MenuItem("Manage", false, manageUserClustering()));
  }
  
  private Command manageUserClustering() {
    return new Command() {
      public void execute() {
        
      }
    };
  }

  private String clusteringCaption(Algorithm algorithm) {
    StringBuffer sb = new StringBuffer();
    sb.append("Row -> ");
    sb.append(algorithm.getRowMethod().asParam());
    sb.append(", ");
    sb.append(algorithm.getRowDistance().asParam());
    sb.append(" Col -> ");
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

  /**
   *  Refresh menu items on clusteringListsChanged fired.
   *  Note the events would be also fired when the DataScreen is activated.
   *  [DataScreen#show -> Screen#show -> Screen#lodaState -> DataListenerWidget#lodaState]
   *  
   *  @see otgviewer.client.DataScreen#show()
   */
  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    super.clusteringListsChanged(lists);
    root.clearItems();
    createMenuItem();
  }

}
