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

package t.gwt.viewer.client.network;

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

import t.gwt.viewer.client.Utils;
import t.gwt.viewer.client.dialog.*;
import t.gwt.viewer.client.storage.NamedObjectStorage;
import t.shared.viewer.network.Network;

public class NetworkVisualizationDialog implements LoadNetworkDialog.Delegate {
  private static final String[] injectList = {
      "network-visualization/lib/jquery-3.3.1.min.js", "network-visualization/lib/cytoscape-3.0.12.min.js",
      "network-visualization/lib/cytoscape-context-menus.js", "network-visualization/lib/weaver-1.2.0.min.js",
      "network-visualization/lib/popper-1.14.6.min.js", "network-visualization/lib/cytoscape-popper-1.0.2.js",
      "network-visualization/lib/cytoscape-canvas-3.0.1.js",
      "network-visualization/utils.js", "network-visualization/extensions.js",
      "network-visualization/application.js" };

  protected DialogBox mainDialog, networkNameDialog;
  DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
  HTML uiDiv = new HTML();
  private HandlerRegistration resizeHandler;
  private Logger logger;
  private Delegate delegate;
  private NamedObjectStorage<PackedNetwork> networkStorage;
  
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

  public NetworkVisualizationDialog(Delegate delegate, 
      NamedObjectStorage<PackedNetwork> networkStorage, Logger logger) {
    this.logger = logger;
    this.networkStorage = networkStorage;
    this.delegate = delegate;
    mainDialog = new DialogBox() {
      @Override
      protected void beginDragging(MouseDownEvent event) {
        event.preventDefault();
      }
    };
    uiDiv.getElement().setId("netvizdiv");
  }

  public void initWindow(@Nullable Network network) {
    createPanel();
    exportPendingRequestHandling();

    Utils.loadHTML(GWT.getModuleBaseURL() + "network-visualization/uiPanel.html", new Utils.HTMLCallback() {
      @Override
      protected void setHTML(String html) {
        uiDiv.setHTML(html);
        injectOnce(() -> {
          setupDockPanel();
          storeNetwork(NetworkConversion.convertNetworkToJS(network));
          startVisualization();
        });
      }
    });

    mainDialog.show();
  }

  public void loadNetwork(Network network) {
    storeNetwork(NetworkConversion.convertNetworkToJS(network));
    changeNetwork();
  }

  private ShowOnceDialog messageDialog = new ShowOnceDialog();

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

  /**
   * Show a dialog asking the user to provide a name for the currently displayed
   * network, and save it under that name.
   *
   * @param title the current name of the network, used as the default value of
   *          the text field
   */
  private void showNetworkNameDialog(String title) {
    SaveObjectDialog dialog = new SaveObjectDialog("Please enter a name for the network.", title, 
        networkStorage, name -> {
          saveCurrentNetwork(name);
          mainDialog.hide();
          resizeHandler.removeHandler();
          delegate.onNetworkVisualizationDialogClose();
        }, () ->  {networkNameDialog.hide();});
    networkNameDialog = Utils.displayInPopup("Name entry", dialog, DialogPosition.Center);
  }

  /**
   * Determines the height that should be given to the div used to contain user
   * interface elements above the graph display. The actual value is set in the
   * injected JavaScript.
   */
  private native int getUiHeight() /*-{
    return $wnd.uiHeight();
  }-*/;

  /**
   * Stores a JavaScript network as window.convertedNetwork.
   */
  private static native void storeNetwork(JavaScriptObject network) /*-{
    $wnd.convertedNetwork = network;
  }-*/;

  /**
   * Saves a JavaScript network into local storage.
   */
  public native void saveNetwork(JavaScriptObject network) /*-{
    var delegate = this.@t.gwt.viewer.client.network.NetworkVisualizationDialog::delegate;
    var packedNetwork = @t.gwt.viewer.client.network.PackedNetwork::new(Ljava/lang/String;Ljava/lang/String;)(network.title, JSON.stringify(network));
    delegate.@t.gwt.viewer.client.network.NetworkVisualizationDialog.Delegate::saveNetwork(Lt/gwt/viewer/client/network/PackedNetwork;)(packedNetwork);
  }-*/;

  public static native String currentNetworkName() /*-{
    return $wnd.vizNet[0].getName();
  }-*/;

  /**
   * Saves the currently displayed network, under the provided title, into local
   * storage.
   */
  public native void saveCurrentNetwork(String title) /*-{
    $wnd.vizNet[0].setName(title);
    this.@t.gwt.viewer.client.network.NetworkVisualizationDialog::saveNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)($wnd.vizNet[0].getNetwork());
  }-*/;

  /**
   * Exports globally available JavaScript methods,
   * window.add/removePendingRequests, that control the display of Toxygates's
   * universal "Please wait..." modal dialog.
   */
  private native void exportPendingRequestHandling() /*-{
    var delegate = this.@t.gwt.viewer.client.network.NetworkVisualizationDialog::delegate;
    $wnd.addPendingRequest = $entry(function() {
      delegate.@t.gwt.viewer.client.network.NetworkVisualizationDialog.Delegate::addPendingRequest()();
    });
    $wnd.removePendingRequest = $entry(function() {
      delegate.@t.gwt.viewer.client.network.NetworkVisualizationDialog.Delegate::removePendingRequest()();
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
    storeNetwork(network.unpackJS());
    showNetworkOnRight();
  }

  protected static native void showNetworkOnRight() /*-{
    $wnd.showNetworkOnRight();
  }-*/;
}
