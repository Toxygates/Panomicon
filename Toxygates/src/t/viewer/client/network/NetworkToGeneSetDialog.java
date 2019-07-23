package t.viewer.client.network;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.ImportingScreen;
import t.viewer.client.Utils;
import t.viewer.shared.StringList;
import t.viewer.shared.network.Network;

public class NetworkToGeneSetDialog {
  protected DialogBox mainDialog = new DialogBox();
  private ListBox probeTypeSelector = new ListBox();
  private ImportingScreen screen;
  
  private Network network;
  private String lastSuggestedName = null;
  
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
    input.setWidth("90%");
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
    
    probeTypeSelector.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        if (input.getValue() == lastSuggestedName) {
          input.setValue(suggestName());
        }
      }
    });
    
    Button saveButton = new Button("Save");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (screen.geneSets().validateNewObjectName(input.getValue(), false)) {
          String[] probes = network.nodes().stream().
              filter(n -> n.type() == probeTypeSelector.getSelectedItemText()).
              map(n -> n.id()).collect(Collectors.toList()).toArray(new String[0]);
          StringList newGeneSet = new StringList("probes", input.getValue(), probes);
          screen.geneSets().put(newGeneSet);
          screen.geneSetsChanged();
          mainDialog.hide();
        }
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

    input.setValue(suggestName());
    
    mainDialog.show();
  }
  
  private String suggestName() {
    String prefix = network.title() + " " + probeTypeSelector.getSelectedItemText();
    lastSuggestedName = screen.geneSets().suggestName(prefix);
    Logger.getLogger("aoeu").info("lastSuggestedName = " + lastSuggestedName);
    return lastSuggestedName;
  }
}
