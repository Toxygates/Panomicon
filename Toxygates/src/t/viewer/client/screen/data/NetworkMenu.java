/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.viewer.client.screen.data;

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import t.viewer.client.components.MenuItemCaptionSeparator;
import t.viewer.client.network.PackedNetwork;
import t.viewer.shared.network.Format;

import java.util.List;

public class NetworkMenu {
  private MenuBar root;
  private MenuItem mi;
  private Delegate delegate;

  public interface Delegate {
    void visualizeNetwork();
    void deleteNetwork(PackedNetwork network);
    void visualizeNetwork(PackedNetwork network);
    void saveProbesAsGeneSet(PackedNetwork network);
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
      item.addItem(new MenuItem("Save probes as gene set", false, () -> delegate.saveProbesAsGeneSet(network)));
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
