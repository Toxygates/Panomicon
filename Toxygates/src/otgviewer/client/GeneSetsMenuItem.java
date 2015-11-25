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
import t.common.shared.ItemList;
import t.common.shared.SharedUtils;
import t.common.shared.StringList;

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
    root.addSeparator(new MenuItemCaptionSeparator("Gene List"));
    
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

    if (hasUserClustering()) {
    }

    if (hasPredefinedClustering()) {
    }

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
