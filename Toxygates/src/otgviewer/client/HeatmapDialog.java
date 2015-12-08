/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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
package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.targetmine.TargetMineData;
import t.common.shared.ClusteringList;
import t.common.shared.StringList;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.common.shared.userclustering.Algorithm;
import t.common.shared.userclustering.Distances;
import t.common.shared.userclustering.Methods;
import t.viewer.client.rpc.MatrixServiceAsync;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class HeatmapDialog extends DataListenerWidget {
  private static final String[] injectList =
      {"kinetic-v5.1.0.min.js", "jquery-2.0.3.min.js", "inchlib-1.2.0.js", "inchlib-extended.js"};

  private final MatrixServiceAsync matrixService;
  protected final Screen screen;

  private DialogBox dialog;
  private Button saveButton;
  private final ListBox valType;

  private CheckBox chkLogAxis;
  private CheckBox chkDendrogram;

  private ListBox rDist;
  private ListBox rMethod;
  private ListBox cDist;
  private ListBox cMethod;

  private Algorithm lastClusteringAlgorithm = new Algorithm();

  public HeatmapDialog(Screen screen) {
    matrixService = screen.matrixService();
    this.screen = screen;
    dialog = new DialogBox();
    valType = new ListBox();
  }

  private void initWindow(ValueType defaultType) {
    createPanel(defaultType);
    inject(new ArrayList<String>(Arrays.asList(injectList)));

    // call DialogBox#show here in order to generate <div> container used by
    // InCHlib.js
    // but keep the dialog invisible until drawing heat map is finished
    dialog.show();
    dialog.setVisible(false);
  }
  
  public static void show(Screen screen, ValueType defaultType) {
    HeatmapDialog dialog = new HeatmapDialog(screen);
    show(dialog, screen, defaultType);    
  }
  
  public static void show(HeatmapDialog dialog, Screen screen, ValueType defaultType) {    
    screen.propagateTo(dialog);
    
    int probesCount = (dialog.chosenProbes != null ? dialog.chosenProbes.length : 0);
    if (probesCount == 0 || probesCount > 1000) {
      Window.alert("Please choose at most 1,000 probes.");
      return;
    } 
    if (probesCount < 2) {
      Window.alert("Please choose at least 2 probes.");
      return;
    } 
    int columnsCount = dialog.chosenColumns.size();
    if (columnsCount < 2) {
      Window.alert("Please define at least 2 columns.");
      return;
    } 
    if (columnsCount > 1000) {
      Window.alert("Please define at most 1,000 columns.");
      return;
    }
    
    // all check passed
    dialog.initWindow(defaultType);;
  }

  private void inject(final List<String> p_jsList) {
    final String js = GWT.getModuleBaseForStaticFiles() + p_jsList.remove(0);

    ScriptInjector.fromUrl(js).setCallback(new Callback<Void, Exception>() {
      @Override
      public void onFailure(Exception e) {
        logger.severe("Script load failed. (" + js + ")");
      }

      @Override
      public void onSuccess(Void ok) {
        if (!p_jsList.isEmpty()) {
          inject(p_jsList);
        } else {
          initializeHeatmap();
          executeClustering(lastClusteringAlgorithm);
        }
      }
    }).setWindow(ScriptInjector.TOP_WINDOW).inject();
  }

  private void initializeHeatmap() {
    createInstance();

    dialog.setGlassEnabled(false);
    dialog.setModal(true);
    dialog.center();
    dialog.setVisible(true);
  }
  
  private void executeClustering(Algorithm algo) {
    logger.info("Execute clustering with " + getValueType().name() + " " + algo.toString());
    this.lastClusteringAlgorithm = algo;
    matrixService.prepareHeatmap(columnsForClustering(chosenColumns), 
        chosenProbes, getValueType(), algo,
        prepareHeatmapCallback());
  }
  
  protected List<Group> columnsForClustering(List<Group> inputs) {
    return chosenColumns;
  }
  
  private AsyncCallback<String> prepareHeatmapCallback() {
    return new PendingAsyncCallback<String>(this) {
      public void handleSuccess(String result) {
        try {
          draw(JsonUtils.safeEval(result));
          updateUI();
        } catch (Exception e) {
          handleFailure(e);
          return;
        }
      }

      public void handleFailure(Throwable caught) {
        logger.severe(caught.getMessage());
        Window.alert("Failed to generate heat map data.");
      }
    };
  }

  private ValueType getValueType() {
    String vt = valType.getItemText(valType.getSelectedIndex());
    return ValueType.unpack(vt);
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
  
  private void createPanel(ValueType defaultType) {
    final ScrollPanel mainContent = new ScrollPanel(); 
    mainContent.setPixelSize(mainWidth(), mainHeight());
    mainContent.setWidget(new HTML("<div id=\"inchlib\"></div>"));
    Window.addResizeHandler(new ResizeHandler() {
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
        Algorithm algo = new Algorithm(Methods.lookup(rMethod.getSelectedValue()),
            Distances.lookup(rDist.getSelectedValue()), Methods.lookup(cMethod.getSelectedValue()),
            Distances.lookup(cDist.getSelectedValue()));

        executeClustering(algo);
      }
    });

    eastContent.add(new Label(" "));
    eastContent.add(updateButton);

    HorizontalPanel topContent = new HorizontalPanel();
    topContent.setSpacing(4);
    topContent.add(new Label("Value:"));

    for (ValueType v : ValueType.values()) {
      valType.addItem(v.toString());
    }
    valType.setSelectedIndex(defaultType.ordinal());
    valType.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        Algorithm algo = new Algorithm(Methods.lookup(rMethod.getSelectedValue()),
            Distances.lookup(rDist.getSelectedValue()), Methods.lookup(cMethod.getSelectedValue()),
            Distances.lookup(cDist.getSelectedValue()));

        executeClustering(algo);
      }
    });
    topContent.add(valType);

    FlowPanel buttonGroup = new FlowPanel();
    HorizontalPanel bottomContent = new HorizontalPanel();
    bottomContent.setWidth("100%");
    bottomContent.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    Button btnEnrich = new Button("Enrichment...");
    btnEnrich.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        HeatmapDialog.this.doEnrichment();
      }
    });
    buttonGroup.add(btnEnrich);

    Button btnClose = new Button("Close");
    btnClose.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        HeatmapDialog.this.dialog.hide();
      }
    });
    buttonGroup.add(btnClose);

    saveButton = new Button("Save as gene set...", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        List<Collection<String>> objectIds = parse2dJsArray(getCurrentObjectIds());

        ClusteringListsStoreHelper helper = new ClusteringListsStoreHelper("userclustering", screen) {
          @Override
          protected void onSaveSuccess(String name, ClusteringList items) {
            Window.alert("Clusters are successfully saved.");
          }
        };
        helper.save(objectIds, lastClusteringAlgorithm);
      }
    });
    buttonGroup.add(saveButton);
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
      $wnd.widget.@otgviewer.client.HeatmapDialog::selectionStateChanged(Ljava/lang/String;)(state);
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

  private void selectionStateChanged(String state) {
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

    saveButton.setEnabled(enabled);
  }

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

  private void doEnrichment() {
    TargetMineData tm = new TargetMineData(screen);
    List<Collection<String>> clusters = parse2dJsArray(getCurrentObjectIds());
    List<StringList> clusterLists = new ArrayList<StringList>();
    int i = 0;
    for (Collection<String> clust : clusters) {
      StringList sl = new StringList("probes", "Cluster " + i, clust.toArray(new String[0]));
      clusterLists.add(sl);
      i++;
    }
    tm.multiEnrich(clusterLists.toArray(new StringList[0]));
  }

}
