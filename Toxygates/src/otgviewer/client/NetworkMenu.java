package otgviewer.client;

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

import t.viewer.client.table.DualTableView;
import t.viewer.shared.network.Format;

public class NetworkMenu {
  private MenuBar root;
  private MenuItem mi;
  private DualTableView dualTable;

  public NetworkMenu(DualTableView dualTable) {
    this.dualTable = dualTable;
    root = new MenuBar(true);
    mi = new MenuItem("Network", false, root);

    createMenuItem();
  }

  public MenuItem menuItem() {
    return mi;
  }

  private void createMenuItem() {
    root.addItem(new MenuItem("Visualize network", () -> dualTable.visualizeNetwork()));
    root.addItem(new MenuItem("Download interaction network (DOT)...", () -> dualTable.downloadNetwork(Format.DOT)));
    root.addItem(new MenuItem("Download interaction network (SIF)...", () -> dualTable.downloadNetwork(Format.SIF)));
    root.addItem(
        new MenuItem("Download interaction network (Custom)...", () -> dualTable.downloadNetwork(Format.Custom)));
  }
}
