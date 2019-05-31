package t.viewer.client.network;

import java.util.List;
import java.util.logging.Logger;

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
