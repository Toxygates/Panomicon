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

package t.viewer.client.network;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

import t.viewer.client.Utils;

public class LoadNetworkDialog {
  protected DialogBox mainDialog = new DialogBox();
  private ListBox networkSelector = new ListBox();
  private Delegate delegate;

  public interface Delegate {
    List<PackedNetwork> networks();
    void loadNetwork(PackedNetwork network);
  }

  public LoadNetworkDialog(Delegate delegate) {
    this.delegate = delegate;
  }

  public void initWindow() {
    mainDialog.setText("Network visualization");

    for (PackedNetwork network : delegate.networks()) {
      networkSelector.addItem(network.title());
    }

    Button loadButton = new Button("Load");
    loadButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        PackedNetwork networkToLoad =
            delegate.networks().get(networkSelector.getSelectedIndex());
        delegate.loadNetwork(networkToLoad);
        mainDialog.hide();
      }
    });

    Button cancelButton = new Button("Cancel");
    cancelButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        mainDialog.hide();
      }
    });

    Panel buttonPanel = Utils.mkHorizontalPanel(true, loadButton, cancelButton);
    Panel verticalPanel = Utils.mkVerticalPanel(true, networkSelector, buttonPanel);

    mainDialog.setWidget(verticalPanel);
    mainDialog.center();
    mainDialog.setModal(true);

    mainDialog.show();
  }
}
