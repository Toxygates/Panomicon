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
package t.viewer.client.clustering;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import t.viewer.client.Utils;
import t.shared.viewer.clustering.Algorithm;
import t.shared.viewer.clustering.Distances;
import t.shared.viewer.clustering.Methods;

import java.util.*;
import java.util.logging.Logger;

/**
 * GUI for configuring, requesting, and displaying a heatmap.
 * Users should subclass this and implement the necessary abstract methods.
 * The GUI is displayed by calling initWindow.
 */
abstract public class HeatmapDialog<C, R> {
  private static final String[] injectList =
      {"kinetic-v5.1.0.min.js", "jquery-2.0.3.min.js", "inchlib-1.2.0.min.js", "inchlib-extended-1.0.0.min.js"};

  protected final ClusteringServiceAsync<C, R> clusteringService;

  // Determines the number of digits after the decimal point to show in heatmap tooltips
  public static final int HEATMAP_TOOLTIP_DECIMAL_DIGITS = 5;

  protected DialogBox dialog;

  private CheckBox chkLogAxis;
  private CheckBox chkDendrogram;

  private ListBox rDist;
  private ListBox rMethod;
  private ListBox cDist;
  private ListBox cMethod;

  protected Logger logger;
  
  protected String matrixId;

  protected Algorithm lastClusteringAlgorithm = new Algorithm();

  private HandlerRegistration resizeHandler;

  public HeatmapDialog(String matrixId, Logger logger, ClusteringServiceAsync<C, R> service) {
    clusteringService = service;
    dialog = new DialogBox() {
      @Override
      protected void beginDragging(MouseDownEvent event) {
        event.preventDefault();
      }
    };
    this.logger = logger;
    this.matrixId = matrixId;
  }

  private static boolean injected = false;

  public void initWindow() {
    createPanel();

    // call DialogBox#show here in order to generate <div> container used by
    // InCHlib.js
    // but keep the dialog invisible until drawing heat map is finished
    dialog.show();
    dialog.setVisible(false);
    injectOnceAndBeginClustering();
  }

  private void injectOnceAndBeginClustering() {
    if (!injected) {
      Utils.inject(new ArrayList<String>(Arrays.asList(injectList)), logger, () -> beginClustering());
      injected = true;
    } else {
      beginClustering();
    }
  }

  private void beginClustering() {
    initializeHeatmap();
    executeClustering(lastClusteringAlgorithm);
  }

  private void initializeHeatmap() {
    createInstance();

    dialog.setGlassEnabled(false);
    dialog.setModal(true);
    dialog.center();
    dialog.setVisible(true);
  }

  protected void executeClustering(Algorithm algo) {
    logger.info("Execute clustering with " + algo.toString());
    this.lastClusteringAlgorithm = algo;
    doClustering(algo);
  }

  protected void doClustering(Algorithm algo) {
    clusteringService.prepareHeatmap(matrixId, columnsForClustering(),
      rowsForClustering(), algo,
        HEATMAP_TOOLTIP_DECIMAL_DIGITS, prepareHeatmapCallback());
  }

  /**
   * Subclasses should implement this to supply the rows that should be included in the clustering.
   * @return
   */
  abstract protected List<R> rowsForClustering();

  /**
   * Subclasses should implement this to supply the columns that should be included in the
   * clustering.
   * @return
   */
  abstract protected List<C> columnsForClustering();

  protected AsyncCallback<String> prepareHeatmapCallback() {
    return new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.severe(caught.toString());
        Window.alert("Failed to generate heat map data.");
      }

      @Override
      public void onSuccess(String result) {
        try {
          draw(JsonUtils.safeEval(result));
          updateUI();
        } catch (Exception e) {
          onFailure(e);
        }
      }
    };
  }

  private void updateUI() {
    boolean b = getDendrogramState();
    chkDendrogram.setValue(b);

    chkLogAxis.setEnabled(b);
    chkLogAxis.setValue(getAxisState());

    rMethod.setEnabled(b);
    rDist.setEnabled(b);
    cMethod.setEnabled(b);
    cDist.setEnabled(b);

    rMethod.setSelectedIndex(lastClusteringAlgorithm.getRowMethod().ordinal());
    rDist.setSelectedIndex(lastClusteringAlgorithm.getRowDistance().ordinal());
    cMethod.setSelectedIndex(lastClusteringAlgorithm.getColMethod().ordinal());
    cDist.setSelectedIndex(lastClusteringAlgorithm.getColDistance().ordinal());
  }

  protected int mainWidth() {
    return Window.getClientWidth() - 160;
  }

  protected int mainHeight() {
    return Window.getClientHeight() - 120;
  }

  private void createPanel() {
    final ScrollPanel mainContent = new ScrollPanel();
    mainContent.setPixelSize(mainWidth(), mainHeight());
    mainContent.setWidget(new HTML("<div id=\"inchlib\"></div>"));
    resizeHandler = Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        int width = mainWidth();
        int height = mainHeight();
        mainContent.setPixelSize(width, height);
        redraw(width, height);
      }
    });

    VerticalPanel eastContent = new VerticalPanel();
    eastContent.setSpacing(4);

    Label l = new Label("Settings");
    l.addStyleName("heading");
    eastContent.add(l);

    chkDendrogram = new CheckBox("Dendrogram");
    chkDendrogram.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean b = chkDendrogram.getValue();
        setDendrogramState(b);
        setColumnDendrogramState(b);
        redraw();
        updateUI();
      }
    });
    eastContent.add(chkDendrogram);

    chkLogAxis = new CheckBox("Log-axis");
    chkLogAxis.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean b = chkLogAxis.getValue();
        setAxisState(b);
        redraw();
        updateUI();
      }
    });
    eastContent.add(chkLogAxis);

    l = new Label("Row");
    l.addStyleName("emphasized");
    eastContent.add(l);

    l = new Label("Method:");
    eastContent.add(l);

    rDist = new ListBox();
    for (Distances d : Distances.values()) {
      rDist.addItem(d.asParam());
    }

    rMethod = new ListBox();
    for (Methods m : Methods.values()) {
      rMethod.addItem(m.asParam());
    }
    eastContent.add(rMethod);

    l = new Label("Distance:");
    eastContent.add(l);
    eastContent.add(rDist);

    l = new Label("Column");
    l.addStyleName("emphasized");
    eastContent.add(l);

    l = new Label("Method:");
    eastContent.add(l);

    cDist = new ListBox();
    for (Distances d : Distances.values()) {
      cDist.addItem(d.asParam());
    }

    cMethod = new ListBox();
    for (Methods m : Methods.values()) {
      cMethod.addItem(m.asParam());
    }
    eastContent.add(cMethod);

    l = new Label("Distance:");
    eastContent.add(l);
    eastContent.add(cDist);

    final Button updateButton = new Button("Update");
    updateButton.setWidth("100%");
    updateButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        recluster();
      }
    });

    eastContent.add(new Label(" "));
    eastContent.add(updateButton);

    HorizontalPanel topContent = new HorizontalPanel();
    topContent.setSpacing(4);
    addTopContent(topContent);

    FlowPanel buttonGroup = new FlowPanel();
    addButtons(buttonGroup);
    HorizontalPanel bottomContent = new HorizontalPanel();
    bottomContent.setWidth("100%");
    bottomContent.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    bottomContent.add(buttonGroup);

    HorizontalPanel hp = new HorizontalPanel();
    hp.add(mainContent);
    hp.add(eastContent);

    VerticalPanel vp = new VerticalPanel();
    vp.add(topContent);
    vp.add(hp);
    vp.add(bottomContent);

    DecoratorPanel dp = new DecoratorPanel();
    dp.add(vp);

    vp = new VerticalPanel();
    vp.add(dp);

    dialog.setText("Heatmap");
    dialog.setWidget(vp);
  }

  protected void recluster() {
    Algorithm algo = new Algorithm(Methods.lookup(rMethod.getSelectedValue()),
      Distances.lookup(rDist.getSelectedValue()), Methods.lookup(cMethod.getSelectedValue()),
      Distances.lookup(cDist.getSelectedValue()));

    executeClustering(algo);
  }

  protected void addTopContent(HorizontalPanel topContent) { }

  protected void addButtons(FlowPanel buttonGroup) {
    Button btnClose = new Button("Close");
    btnClose.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        HeatmapDialog.this.dialog.hide();
        resizeHandler.removeHandler();
      }
    });
    buttonGroup.add(btnClose);
  }

  protected List<Collection<String>> getCurrent2DArray() {
    return parse2dJsArray(getCurrentObjectIds());
  }

  private native JsArray<JsArrayString> getCurrentObjectIds() /*-{
    return $wnd.inchlib.get_current_object_ids();
  }-*/;

  private native boolean getDendrogramState() /*-{
    return $wnd.inchlib.settings.dendrogram;
  }-*/;

  private native boolean getColumnDendrogramState() /*-{
    return $wnd.inchlib.settings.column_dendrogram;
  }-*/;

  private native boolean getAxisState() /*-{
    return $wnd.inchlib.settings.log_axis;
  }-*/;

  private native void setDendrogramState(boolean b)/*-{
    $wnd.inchlib.settings.dendrogram = b;
  }-*/;

  private native void setColumnDendrogramState(boolean b)/*-{
    $wnd.inchlib.settings.column_dendrogram = b;
  }-*/;

  private native void setAxisState(boolean b)/*-{
    $wnd.inchlib.settings.log_axis = b;
  }-*/;

  private native void draw(JavaScriptObject json)/*-{
    $wnd.inchlib.read_data(json);
    $wnd.inchlib.draw();
  }-*/;

  private native void redraw()/*-{
    $wnd.inchlib.redraw();
  }-*/;

  private native void redraw(int w, int h)/*-{
    $wnd.inchlib.settings.width = w;
    $wnd.inchlib.settings.max_height = h;
    $wnd.inchlib.redraw();
  }-*/;

  private native void createInstance()/*-{
    $wnd.widget = this;

    $wnd.inchlib = new $wnd.InCHlibEx({
      target : "inchlib",
      dendrogram : true,
      metadata : false,
      column_metadata : false,
      heatmap_colors : "BuWhRd",
      heatmap_font_color : "black",
      column_dendrogram : true,
      independent_columns : false,
      highlight_colors : "Oranges",
      label_color : "#9E9E9E",
      min_row_height : 20,
      max_column_width : 20,
      font : "Helvetica",
      draw_row_ids : true,
      fixed_row_id_size : 14,
      navigation_toggle : {
        "color_scale" : true,
        "distance_scale" : true,
        "export_button" : true,
        "filter_button" : false,
        "hint_button" : false
      },
      log_axis : true,
    });

    $wnd.inchlib.selection_state_changed = function(state) {
      $wnd.widget.@t.viewer.client.clustering.HeatmapDialog::selectionStateChanged(Ljava/lang/String;)(state);
    }

    $wnd.inchlib.get_current_object_ids = function() {
      var self = this;
      switch (self.current_selection_state) {
      case self.selection_state.range:
      case self.selection_state.dendrogram:
        return [ self.current_object_ids ];
      case self.selection_state.cutoff:
        return self.current_object_ids;
      default:
        return [ [] ];
      }
    }
  }-*/;

  protected void selectionStateChanged(String state) {
    boolean enabled = false;

    if (state != null) {
      switch (state.trim().toLowerCase()) {
        case "range":
        case "dendrogram":
        case "cutoff":
          enabled = true;
          break;
        default:
          enabled = false;
      }
    }

    guiStateChanged(enabled);
  }

  /**
   * Hook for enabling/disabling buttons and controls while the clustering is in progress
   */
  protected void guiStateChanged(boolean enabled) { }

  private List<Collection<String>> parse2dJsArray(JsArray<JsArrayString> array) {
    List<Collection<String>> result = new LinkedList<Collection<String>>();
    int size = array.length();
    for (int i = 0; i < size; ++i) {
      result.add(parseJsArrayString(array.get(i)));
    }
    return result;
  }

  private List<String> parseJsArrayString(JsArrayString array) {
    List<String> result = new LinkedList<String>();
    int size = array.length();
    for (int i = 0; i < size; ++i) {
      result.add(array.get(i));
    }
    return result;
  }

}
