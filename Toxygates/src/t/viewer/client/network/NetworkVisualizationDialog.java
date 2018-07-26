package t.viewer.client.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.viewer.client.Utils;
import t.viewer.shared.network.Network;

public class NetworkVisualizationDialog {
  private static final String[] injectList = { "network-visualization/cytoscape-cose-bilkent.js",
      "network-visualization/cytoscape.min.js", "network-visualization/graphicNode.js",
      "network-visualization/interaction.js", "network-visualization/network.js", "network-visualization/node.js",
      "network-visualization/utils.js", "network-visualization/vis.min.js",
      "network-visualization/application.js" };

  protected DialogBox dialog;
  DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
  HTML uiDiv = new HTML();
  private HandlerRegistration resizeHandler;
  private Logger logger;
  private Delegate delegate;

  private static Boolean injected = false;

  public interface Delegate {
    void saveNetwork(Network network);
    //List<Network> networks();
  }

  public NetworkVisualizationDialog(Delegate delegate, Logger logger) {
    this.logger = logger;
    this.delegate = delegate;
    dialog = new DialogBox() {
      @Override
      protected void beginDragging(MouseDownEvent event) {
        event.preventDefault();
      };
    };
  }

  public void initWindow(@Nullable Network network) {
    createPanel();
    exportSaveNetwork();

    Utils.loadHTML(GWT.getModuleBaseURL() + "network-visualization/uiPanel.html", new Utils.HTMLCallback() {
      @Override
      protected void setHTML(String html) {
        uiDiv.setHTML(html);
        injectOnce(() -> {
          setupDockPanel();
          convertAndStoreNetwork(network);
          startVisualization();
        });
      }
    });

    dialog.show();
  }

  private void injectOnce(final Runnable callback) {
    if (!injected) {
      Utils.inject(new ArrayList<String>(Arrays.asList(injectList)), logger, callback);
      injected = true;
    } else {
      callback.run();
    }
  }

  protected int mainWidth() {
    return Window.getClientWidth() - 44;
  }

  protected int mainHeight() {
    return Window.getClientHeight() - 62;
  }

  private void createPanel() {
    dialog.setText("Network visualization");

    dockPanel.setPixelSize(mainWidth(), mainHeight());

    resizeHandler = Window.addResizeHandler((ResizeEvent event) -> {
      dockPanel.setPixelSize(mainWidth(), mainHeight());
    });
    dialog.setWidget(dockPanel);
    dialog.center();
    dialog.setModal(true);
  }

  /**
   * Sets up the dock panel. Needs to happen later so that ui panel height can
   * be fetched from injected JavaScript.
   */
  private void setupDockPanel() {
    FlowPanel buttonGroup = new FlowPanel();

    Button btnClose = new Button("Close");
    btnClose.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        NetworkVisualizationDialog.this.dialog.hide();
        resizeHandler.removeHandler();
      }
    });
    buttonGroup.add(btnClose);

    Button btnSave = new Button("Save and close");
    btnSave.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        saveCurrentNetwork();
        NetworkVisualizationDialog.this.dialog.hide();
        resizeHandler.removeHandler();
      }
    });
    buttonGroup.add(btnSave);

    dockPanel.addNorth(uiDiv, getUiHeight());
    dockPanel.addSouth(buttonGroup, 27);

    SimplePanel displayPanel = new SimplePanel();
    displayPanel.setStyleName("visualization");
    displayPanel.getElement().setId("display");
    dockPanel.add(displayPanel);
  }

  private native int getUiHeight() /*-{
    return $wnd.uiHeight();
  }-*/;

  /**
   * Converts a Java network to a JavaScript network , and stores it as
   * window.convertedNetwork.
   */
  private static native void convertAndStoreNetwork(Network network) /*-{
    $wnd.convertedNetwork = @t.viewer.client.network.NetworkConversion::convertNetworkToJS(Lt/viewer/shared/network/Network;)(network);
  }-*/;

  /**
   * Handles the logic for actually saving a network from the visualization dialog
   */
  public native void saveNetwork(JavaScriptObject network) /*-{
    var javaNetwork = @t.viewer.client.network.NetworkConversion::convertNetworkToJava(Lcom/google/gwt/core/client/JavaScriptObject;)(network);
    var delegate = this.@t.viewer.client.network.NetworkVisualizationDialog::delegate;
    delegate.@t.viewer.client.network.NetworkVisualizationDialog.Delegate::saveNetwork(Lt/viewer/shared/network/Network;)(javaNetwork);
  }-*/;

  public native void saveCurrentNetwork() /*-{
    this.@t.viewer.client.network.NetworkVisualizationDialog::saveNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)($wnd.toxyNet);
  }-*/;

  /**
   * Exports the saveNetwork method to the window so that it can be called from hand-written
   * JavaScript.
   */
  public native void exportSaveNetwork() /*-{
    var that = this;
    $wnd.saveNetworkToToxygates = $entry(function(network, jsonString) {
      that.@t.viewer.client.network.NetworkVisualizationDialog::saveNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)(network);
    });
  }-*/;

  /**
   * Called after UI HTML has been loaded, all scripts have been injected, and
   * network has been converted and stored in the JavaScript window, to inform the
   * visualization system that it should start drawing the network.
   */
  private native void startVisualization() /*-{
    $wnd.onReadyForVisualization();
  }-*/;
}
