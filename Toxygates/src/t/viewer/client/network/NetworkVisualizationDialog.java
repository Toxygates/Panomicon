package t.viewer.client.network;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.InputDialog;
import t.viewer.shared.network.Network;

public class NetworkVisualizationDialog implements LoadNetworkDialog.Delegate {
  private static final String[] injectList = {
      "network-visualization/lib/jquery-3.3.1.min.js", "network-visualization/lib/cytoscape.min.js",
      "network-visualization/lib/cytoscape-context-menus.js", "network-visualization/lib/weaver-1.2.0.min.js",
      "network-visualization/toxyNode.js",
      "network-visualization/interaction.js", "network-visualization/network.js",
      "network-visualization/utils.js", "network-visualization/extensions.js",
      "network-visualization/application.js" };

  protected DialogBox mainDialog, networkNameDialog;
  DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
  HTML uiDiv = new HTML();
  private HandlerRegistration resizeHandler;
  private Logger logger;
  private Delegate delegate;

  private static Boolean injected = false;

  public interface Delegate {
    void saveNetwork(PackedNetwork network);
    void showMirnaSourceDialog();
    FilterEditPanel filterEditPanel();
    void onNetworkVisualizationDialogClose();
    void addPendingRequest();
    void removePendingRequest();
    List<PackedNetwork> networks();
  }

  public NetworkVisualizationDialog(Delegate delegate, Logger logger) {
    this.logger = logger;
    this.delegate = delegate;
    mainDialog = new DialogBox() {
      @Override
      protected void beginDragging(MouseDownEvent event) {
        event.preventDefault();
      };
    };
    uiDiv.getElement().setId("netvizdiv");
  }

  public void initWindow(@Nullable Network network) {
    createPanel();
    exportPendingRequsetHandling();

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

    mainDialog.show();
  }

  public void loadNetwork(Network network) {
    convertAndStoreNetwork(network);
    changeNetwork();
  }

  private void injectOnce(final Runnable callback) {
    if (!injected) {
      loadCss(GWT.getModuleBaseURL() + "network-visualization/style.css");
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
    mainDialog.setText("Network visualization");

    dockPanel.setPixelSize(mainWidth(), mainHeight());

    resizeHandler = Window.addResizeHandler((ResizeEvent event) -> {
      dockPanel.setPixelSize(mainWidth(), mainHeight());
    });
    mainDialog.setWidget(dockPanel);
    mainDialog.center();
    mainDialog.setModal(true);
  }

  /**
   * Sets up the dock panel. Needs to happen later so that ui panel height can
   * be fetched from injected JavaScript.
   */
  private void setupDockPanel() {
    Button closeButton = new Button("Close");
    closeButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        mainDialog.hide();
        resizeHandler.removeHandler();
        delegate.onNetworkVisualizationDialogClose();
      }
    });

    Button saveButton = new Button("Save and close");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showNetworkNameDialog(currentNetworkName());
      }
    });

    Button mirnaButton = new Button("Change miRNA sources");
    mirnaButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        delegate.showMirnaSourceDialog();
      }
    });

    Button loadNetworkButton = new Button("Load additional network");
    loadNetworkButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        new LoadNetworkDialog(NetworkVisualizationDialog.this).initWindow();
      }
    });

    FilterEditPanel filterEditPanel = delegate.filterEditPanel();
    Panel buttonPanel = Utils.mkHorizontalPanel(true, closeButton, saveButton, mirnaButton,
        filterEditPanel.content(), loadNetworkButton);

    dockPanel.addNorth(uiDiv, getUiHeight());
    dockPanel.addSouth(buttonPanel, 35);

    SimplePanel displayPanel = new SimplePanel();
    displayPanel.setStyleName("visualization");
    displayPanel.getElement().setId("display");
    dockPanel.add(displayPanel);
  }

  private void showNetworkNameDialog(String title) {
    InputDialog entry = new InputDialog("Please enter a name for the network.", title) {
      @Override
      protected void onChange(String value) {
        if (value != "") { // Empty string means OK button with blank text input
          networkNameDialog.hide();
          if (value != null) { // value == null means cancel button
            saveCurrentNetwork(value);
            mainDialog.hide();
            resizeHandler.removeHandler();
            delegate.onNetworkVisualizationDialogClose();
          }
        }
      }
    };
    networkNameDialog = Utils.displayInPopup("Name entry", entry, DialogPosition.Center);
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
    var delegate = this.@t.viewer.client.network.NetworkVisualizationDialog::delegate;
    var packedNetwork = @t.viewer.client.network.PackedNetwork::new(Ljava/lang/String;Ljava/lang/String;)(network.title, JSON.stringify(network));
    delegate.@t.viewer.client.network.NetworkVisualizationDialog.Delegate::saveNetwork(Lt/viewer/client/network/PackedNetwork;)(packedNetwork);
  }-*/;

  public static native String currentNetworkName() /*-{
    return $wnd.toxyNet.title;
  }-*/;

  public native void saveCurrentNetwork(String title) /*-{
    $wnd.updateToxyNet();
    $wnd.toxyNet[0].title = title;
    this.@t.viewer.client.network.NetworkVisualizationDialog::saveNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)($wnd.toxyNet[0]);
  }-*/;

  /**
   * Exports globally available JavaScript methods,
   * window.add/removePendingRequests, that control the display of Toxygates's
   * universal "Please wait..." modal dialog.
   */
  private native void exportPendingRequsetHandling() /*-{
    var delegate = this.@t.viewer.client.network.NetworkVisualizationDialog::delegate;
    $wnd.addPendingRequest = $entry(function() {
      delegate.@t.viewer.client.network.NetworkVisualizationDialog.Delegate::addPendingRequest()();
    });
    $wnd.removePendingRequest = $entry(function() {
      delegate.@t.viewer.client.network.NetworkVisualizationDialog.Delegate::removePendingRequest()();
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

  /**
   * Informs the network visualization that a new network has been placed in
   * $wnd.toxyNet, and that it should be loaded and visualized.
   */
  private native void changeNetwork() /*-{
    $wnd.changeNetwork();
  }-*/;

  /**
   * Injects CSS from a URL
   */
  public static void loadCss(String url) {
    LinkElement link = Document.get().createLinkElement();
    link.setRel("stylesheet");
    link.setHref(url);
    attachToHead(link);
  }

  protected static native void attachToHead(JavaScriptObject scriptElement) /*-{
    $doc.getElementsByTagName("head")[0].appendChild(scriptElement);
  }-*/;

  // LoadNetworkDialog.Delegate methods
  @Override
  public List<PackedNetwork> networks() {
    return delegate.networks();
  }

  @Override
  public void loadNetwork(PackedNetwork network) {
    convertAndStoreNetwork(network.unpack());
    showNetworkOnRight();
  }

  protected static native void showNetworkOnRight() /*-{
    $wnd.showNetworkOnRight();
  }-*/;
}
