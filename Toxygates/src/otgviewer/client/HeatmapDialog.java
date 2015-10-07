package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListChooser;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.ValueType;
import t.viewer.client.rpc.MatrixServiceAsync;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
        Window.alert("Fail to generate heat map data.");
      }
    };
  }

  public ValueType getValueType() {
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

  private void initializeHeatmap() {
    createInstance();
    updateSaveButton("F");
    dialog.setGlassEnabled(false);
    dialog.setModal(true);
    dialog.center();
    dialog.setVisible(true);
  }

  private void createPanel(ValueType defaultType) {
    ScrollPanel mainContent = new ScrollPanel();
    mainContent.setPixelSize(600, 500);
    mainContent.setWidget(new HTML("<div id=\"inchlib\"></div>"));

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

    saveButton = new Button("Save as gene set...");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String mode = getSelectionMode();
        if (mode == null) {
          return;
        }
        
        if (mode.equals("free") || mode.equals("dendrogram")) {
          JsArrayString array = getSelection();
          
          if (array == null) {
            return;
          }

          List<String> chosenProbes = new ArrayList<String>();
          for (int i = 0; i < array.length(); ++i) {
            chosenProbes.add(array.get(i));
          }
          saveSimpleGeneSelection(chosenProbes);
        } else if (mode.equals("cutoff")) {
          
        } else {
          
        }
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
  
  private void saveSimpleGeneSelection(List<String> chosenProbes) {
    ListChooser lc =
        new ListChooser(new ArrayList<StringList>(), "probes") {
          @Override
          protected void listsChanged(List<ItemList> lists) {
            screen.itemListsChanged(lists);
            screen.storeItemLists(screen.getParser());
          }
          @Override
          protected boolean checkName(String name) {
            if (!super.checkName(name)) {
              return false;
            }
            if (containsEntry("probes", name)) {
              Window.alert("The title \"" + name + "\" is already taken.\n"
                  + "Please choose a different name.");
              return false;
            }
            return true;
          }
        };
    // TODO make ListChooser use the DataListener propagate mechanism?
    lc.setLists(screen.chosenItemLists);

    lc.setItems(chosenProbes);
    lc.saveAction();
  }

  private void toggleDendrogram(boolean b) {
    toggleRowDendrogram(b);
    toggleColumnDendrogram(b);
    redraw();
  }

  native JsArrayString getSelection()/*-{    
    var mode = $wnd.inchlib.selection_mode;
    if (!mode) {
      return null;
    }
    
    if (mode == "free") {
      return $wnd.inchlib.selection;
    } else {
      return $wnd.inchlib.current_object_ids;
    }
  }-*/;

  native String getSelectionMode()/*-{
    return $wnd.inchlib.selection_mode;
  }-*/;

  native JavaScriptObject toggleRowDendrogram(boolean b)/*-{
    $wnd.inchlib.settings.dendrogram = b;
  }-*/;

  native JavaScriptObject toggleColumnDendrogram(boolean b)/*-{
    $wnd.inchlib.settings.column_dendrogram = b;
  }-*/;

  native JavaScriptObject redraw()/*-{
    $wnd.inchlib.redraw();
  }-*/;
    
  public void updateSaveButton(String enabled) {
    String b = enabled.trim().toLowerCase();
    if (b.equals("t") || b.equals("true")) {
      saveButton.setEnabled(true);
    } else {
      saveButton.setEnabled(false);
    }
  }

  native JavaScriptObject createInstance()/*-{
    $wnd.widget = this;

    $wnd.inchlib = new $wnd.InCHlibEx({
      target : "inchlib",
      dendrogram : true,
      metadata : false,
      column_metadata : false,
      max_height : 1200,
      width : 600,
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
    
    $wnd.inchlib.selection_mode_changed = function() {
      var self = this;
      if (!self.selection_mode || self.selection_mode == "cutoff") {
        $wnd.widget.@otgviewer.client.HeatmapDialog::updateSaveButton(Ljava/lang/String;)("F");
      } else {
        $wnd.widget.@otgviewer.client.HeatmapDialog::updateSaveButton(Ljava/lang/String;)("T");     
      }
    }
  }-*/;

  native JavaScriptObject draw(JavaScriptObject json)/*-{
    $wnd.inchlib.read_data(json)
    $wnd.inchlib.draw(); //draw cluster heatmap
  }-*/;

}
