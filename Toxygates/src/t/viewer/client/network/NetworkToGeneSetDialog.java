package t.viewer.client.network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import otg.viewer.client.components.ImportingScreen;
import t.viewer.client.Utils;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
import t.viewer.shared.network.Network;

public class NetworkToGeneSetDialog {
  protected DialogBox mainDialog = new DialogBox();
  private ListBox probeTypeSelector = new ListBox();
  private ImportingScreen screen;
  
  private Network network;
  
  public NetworkToGeneSetDialog(Network network, ImportingScreen screen) {
    this.network = network;
    this.screen = screen;
  }
  
  public void initWindow() {
    mainDialog.setText("Extract gene set from " + network.title());
    
    VerticalPanel verticalPanel = new VerticalPanel();
    verticalPanel.setWidth("100%");
    verticalPanel.add(new Label("Name for new gene set:"));

    final TextBox input = new TextBox();
    verticalPanel.add(input);
    
    verticalPanel.add(new Label("Type of probes to extract:"));
    
    Set<String> probeTypeNames = new HashSet<String>();
    network.nodes().forEach(node -> {
      probeTypeNames.add(node.type());
    });
    probeTypeNames.forEach(probeType -> {
      probeTypeSelector.addItem(probeType);
    });
    verticalPanel.add(probeTypeSelector);
    
    Button saveButton = new Button("Save");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String[] probes = network.nodes().stream().
            filter(n -> n.type() == probeTypeSelector.getSelectedItemText()).
            map(n -> n.id()).collect(Collectors.toList()).toArray(new String[0]);
        StringList newGeneSet = new StringList("probes", input.getValue(), probes);
        List<ItemList> itemLists = screen.getStorage().itemListsStorage.getIgnoringException();
        itemLists.removeIf(l -> l.name() == input.getValue());
        itemLists.add(newGeneSet);
        screen.itemListsChanged(itemLists);
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

    Panel buttonPanel = Utils.mkHorizontalPanel(true, saveButton, cancelButton);
    verticalPanel.add(buttonPanel);

    mainDialog.setWidget(verticalPanel);
    mainDialog.center();
    mainDialog.setModal(true);

    mainDialog.show();
  }
}
