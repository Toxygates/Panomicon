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
import t.common.shared.ValueType;
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
  private static final String[] injectList = {"kinetic-v5.1.0.min.js",
      "jquery-2.0.3.min.js", "inchlib-1.2.0.js", "inchlib-extended.js"};

  private final MatrixServiceAsync matrixService;
  private final Screen screen;

  private DialogBox dialog;
  private Button saveButton;
  private final ListBox valType;

  private String json;

  public HeatmapDialog(Screen screen, ValueType defaultType) {
    matrixService = screen.matrixService();
    this.screen = screen;
    dialog = new DialogBox();
    valType = new ListBox();

    screen.propagateTo(this);
    initWindow(defaultType);
  }

  private void initWindow(ValueType defaultType) {
    logger.info("Heatmap.initWindow()");

    createPanel(defaultType);
    inject(new ArrayList<String>(Arrays.asList(injectList)));

    // call DialogBox#show here in order to generate <div> container used by InCHlib.js
    // but keep the dialog invisible until drawing heat map is finished
    dialog.show();
    dialog.setVisible(false);
  }

  private void executeClustering() {
    logger.info("Execute clustering with " + getValueType().name());
    matrixService.prepareHeatmap(chosenColumns, chosenProbes, getValueType(),
        prepareHeatmapCallback());
  }

  private AsyncCallback<String> prepareHeatmapCallback() {
    return new PendingAsyncCallback<String>(this) {
      public void handleSuccess(String result) {
        try {
          draw(JsonUtils.safeEval(result));
        } catch (Exception e) {
          handleFailure(e);
          return;
        }
      }

      public void handleFailure(Throwable caught) {
        logger.severe(caught.getMessage());
        Window.alert("Fail to generate heat map data.");
      }
    };
  }

  private ValueType getValueType() {
    String vt = valType.getItemText(valType.getSelectedIndex());
    return ValueType.unpack(vt);
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
        logger.info("Script load succeeded. (" + js + ")");
        if (!p_jsList.isEmpty()) {
          inject(p_jsList);
        } else {
          initializeHeatmap();
          executeClustering();
        }
      }
    }).setWindow(ScriptInjector.TOP_WINDOW).inject();
  }

  private void onReady() {
    draw(JsonUtils.safeEval(json));
  }

  private native JavaScriptObject draw(JavaScriptObject json)/*-{
    $wnd.inchlib.read_data(json)
    $wnd.inchlib.draw();
  }-*/;

  private void initializeHeatmap() {
    createInstance();
    updateSaveButton("F");
    dialog.setGlassEnabled(false);
    dialog.setModal(true);
    dialog.center();
    dialog.setVisible(true);
  }

  private void createPanel(ValueType defaultType) {
    final ScrollPanel mainContent = new ScrollPanel();
    mainContent.setPixelSize((int) (Window.getClientWidth() * 0.7),
        (int) (Window.getClientHeight() * 0.7));
    mainContent.setWidget(new HTML("<div id=\"inchlib\"></div>"));
    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        int width = (int) (Window.getClientWidth() * 0.7);
        int height = (int) (Window.getClientHeight() * 0.7);
        mainContent.setPixelSize(width, height);
        redraw(width, height);
      }
    });

    VerticalPanel eastContent = new VerticalPanel();
    eastContent.setSpacing(4);

    Label l = new Label("Settings");
    l.addStyleName("heading");
    eastContent.add(l);

    final CheckBox cb = new CheckBox("Dendrogram");
    cb.setValue(true);
    eastContent.add(cb);

    l = new Label("Row");
    l.addStyleName("indent1");
    eastContent.add(l);

    l = new Label("Method:");
    l.addStyleName("indent2");
    eastContent.add(l);

    final ListBox rDist = new ListBox();
    rDist.addItem("Correlation");
    rDist.addStyleName("indent2");

    final ListBox rMethod = new ListBox();
    rMethod.addItem("Ward");
    rMethod.addStyleName("indent2");
    rMethod.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        if (rMethod.getSelectedIndex() == 0) {
          rDist.setEnabled(false);
        } else {
          rDist.setEnabled(true);
        }
      }
    });
    eastContent.add(rMethod);

    l = new Label("Distance:");
    l.addStyleName("indent2");
    eastContent.add(l);
    eastContent.add(rDist);

    l = new Label("Column");
    l.addStyleName("indent1");
    eastContent.add(l);

    l = new Label("Method:");
    l.addStyleName("indent2");
    eastContent.add(l);

    final ListBox cDist = new ListBox();
    cDist.addItem("Correlation");
    cDist.addStyleName("indent2");

    final ListBox cMethod = new ListBox();
    cMethod.addItem("Ward");
    cMethod.addStyleName("indent2");
    cMethod.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        if (cMethod.getSelectedIndex() == 0) {
          cDist.setEnabled(false);
        } else {
          cDist.setEnabled(true);
        }
      }
    });
    eastContent.add(cMethod);

    l = new Label("Distance:");
    l.addStyleName("indent2");
    eastContent.add(l);
    eastContent.add(cDist);

    final Button updateButton = new Button("Update");
    updateButton.setWidth("100%");
    updateButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean b = cb.getValue();

        toggleDendrogram(b);
      }
    });

    cb.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean b = cb.getValue();
        rMethod.setEnabled(b);
        rDist.setEnabled(b);
        cMethod.setEnabled(b);
        cDist.setEnabled(b);
        updateButton.setEnabled(b);
        toggleDendrogram(b);
      }
    });

    eastContent.add(new Label(" "));
    eastContent.add(updateButton);

    HorizontalPanel topContent = new HorizontalPanel();
    topContent.setSpacing(4);
    topContent.add(new Label("Value:"));

    valType.addItem(ValueType.Folds.toString());
    valType.addItem(ValueType.Absolute.toString());
    valType.setSelectedIndex(defaultType.ordinal());
    valType.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        executeClustering();
      }
    });
    topContent.add(valType);

    FlowPanel buttonGroup = new FlowPanel();
    HorizontalPanel bottomContent = new HorizontalPanel();
    bottomContent.setWidth("100%");
    bottomContent.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

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

        ItemListsStoreHelper helper =
            new ItemListsStoreHelper("probes", screen) {
              @Override
              protected void onSaveSuccess(String name, Collection<String> items) {
                Window.alert("Gene sets are successfully saved.");
              }
            };
        helper.save(objectIds);
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

  private void toggleDendrogram(boolean b) {
    toggleRowDendrogram(b);
    toggleColumnDendrogram(b);
    redraw();
  }

  private native JsArray<JsArrayString> getCurrentObjectIds() /*-{
    return $wnd.inchlib.get_current_object_ids();
  }-*/;

  private native JavaScriptObject toggleRowDendrogram(boolean b)/*-{
    $wnd.inchlib.settings.dendrogram = b;
  }-*/;

  private native JavaScriptObject toggleColumnDendrogram(boolean b)/*-{
    $wnd.inchlib.settings.column_dendrogram = b;
  }-*/;

  private native JavaScriptObject redraw()/*-{
    $wnd.inchlib.redraw();
  }-*/;

  private native JavaScriptObject redraw(int w, int h)/*-{
    $wnd.inchlib.settings.width = w;
    $wnd.inchlib.settings.max_height = h;
    $wnd.inchlib.redraw();
  }-*/;

  private void updateSaveButton(String enabled) {
    String b = enabled.trim().toLowerCase();
    if (b.equals("t") || b.equals("true")) {
      saveButton.setEnabled(true);
    } else {
      saveButton.setEnabled(false);
    }
  }

  private native JavaScriptObject createInstance()/*-{
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

}
