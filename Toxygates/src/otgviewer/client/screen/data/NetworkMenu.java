package otgviewer.client.screen.data;

import java.util.List;

import com.google.gwt.user.client.ui.*;

import t.viewer.client.components.MenuItemCaptionSeparator;
import t.viewer.client.network.PackedNetwork;
import t.viewer.shared.network.Format;

public class NetworkMenu {
  private MenuBar root;
  private MenuItem mi;
  private Delegate delegate;

  public interface Delegate {
    void visualizeNetwork();
    void deleteNetwork(PackedNetwork network);
    void visualizeNetwork(PackedNetwork network);
    void downloadNetwork(Format format);
    List<PackedNetwork> networks();
  }

  public NetworkMenu(Delegate delegate) {
    this.delegate = delegate;
    root = new MenuBar(true);
    mi = new MenuItem("Network", false, root);

    createMenuItem();
  }

  public MenuItem menuItem() {
    return mi;
  }

  private void createMenuItem() {
    root.addItem(new MenuItem("Visualize network", () -> delegate.visualizeNetwork()));
    root.addSeparator(new MenuItemCaptionSeparator("Stored networks"));

    for (final PackedNetwork network : delegate.networks()) {
      MenuBar item = new MenuBar(true);

      item.addItem(new MenuItem("Visualize", false, () -> delegate.visualizeNetwork(network)));
      item.addItem(new MenuItem("Delete", false, () -> delegate.deleteNetwork(network)));
      root.addItem(network.title(), item);
    }
    root.addSeparator(new MenuItemSeparator());
    root.addItem(new MenuItem("Download interaction network (DOT)...", () -> delegate.downloadNetwork(Format.DOT)));
    root.addItem(new MenuItem("Download interaction network (SIF)...", () -> delegate.downloadNetwork(Format.SIF)));
    root.addItem(
        new MenuItem("Download interaction network (Custom)...", () -> delegate.downloadNetwork(Format.Custom)));
  }

  public void networksChanged() {
    root.clearItems();
    createMenuItem();
  }
}
