package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.ValueType;
import t.viewer.client.rpc.MatrixServiceAsync;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class HeatmapDialog extends DataListenerWidget {

  private static final String[] injectList = {"kinetic-v5.1.0.min.js",
      "jquery-2.0.3.min.js", "inchlib-1.2.0.js"};

  private final MatrixServiceAsync matrixService;

  private DialogBox dialog;

  private String json;
  
  private final ListBox valType;

  public HeatmapDialog(Screen screen, ValueType defaultType) {
    matrixService = screen.matrixService();
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
    matrixService.prepareHeatmap(chosenColumns, chosenProbes, getValueType(), prepareHeatmapCallback());
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
    this.show();
  }
  
  private void initializeHeatmap() {
    logger.info("Heatmap.initializeHeatmap()");
    createInstance();
    add_color_scale();
    update_draw_row_ids();
    update_draw_heatmap_header();
    dialog.setGlassEnabled(false);
    dialog.setModal(true);
    dialog.center();
    dialog.setVisible(true);
  }

  public void show() {
    logger.info("Heatmap.show()");

//    createInstance();
//    add_color_scale();
//    update_draw_row_ids();
//    update_draw_heatmap_header();
    draw(JsonUtils.safeEval(json));

//    dialog.setGlassEnabled(false);
//    dialog.setModal(true);
//    dialog.center();
//    dialog.setVisible(true);
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
    rDist.addItem("Euclidean");
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
    cDist.addItem("Euclidean");
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
    bottomContent.add(btnClose);

    Button b = new Button("Save as gene set...");
    bottomContent.add(b);

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
    // return vp;
  }

  private void toggleDendrogram(boolean b) {
    toggleRowDendrogram(b);
    toggleColumnDendrogram(b);
    redraw();
  }

  native JavaScriptObject toggleRowDendrogram(boolean b)/*-{
		$wnd.inchlib.settings.dendrogram = b;
		console.log("Toggle row dendrogram: " + b)
  }-*/;

  native JavaScriptObject toggleColumnDendrogram(boolean b)/*-{
		$wnd.inchlib.settings.column_dendrogram = b;
		console.log("Toggle column dendrogram: " + b)
  }-*/;

  native JavaScriptObject redraw()/*-{
		$wnd.inchlib.redraw();
		console.log("Redraw heatmap.")
  }-*/;

  native JavaScriptObject createInstance()/*-{
		$wnd.inchlib = new $wnd.InCHlib({ //instantiate InCHlib
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
		console.log("Init heatmap done.")
  }-*/;

  native JavaScriptObject draw(JavaScriptObject json)/*-{
		$wnd.inchlib.read_data(json)
		$wnd.inchlib.draw(); //draw cluster heatmap
		console.log("Drowing heatmap finished.")
  }-*/;

  native JavaScriptObject update_draw_row_ids()/*-{
		$wnd.inchlib._draw_row_ids = function() {
			var self = this;
			if (self.pixels_for_leaf < 6 || self.row_id_size < 5) {
				return;
			}
			var i, objects, object_y = [], leaf, values = [], text;

			for (i = 0, keys = Object.keys(self.leaves_y_coordinates),
					len = keys.length; i < len; i++) {
				leaf_id = keys[i];
				objects = self.data.nodes[leaf_id].objects;
				if (objects.length > 1) {
					return;
				}
				object_y
						.push([ objects[0], self.leaves_y_coordinates[leaf_id] ]);
			}

			var x = self.distance + self._get_visible_count()
					* self.pixels_for_dimension + 15;

			for (i = 0; i < object_y.length; i++) {
				text = self.objects_ref.heatmap_value
						.clone({
							x : x,
							y : self._hack_round(object_y[i][1]
									- self.row_id_size / 2),
							fontSize : self.row_id_size,
							text : object_y[i][0],
							fill : "black"
						});
				self.heatmap_layer.add(text);
			}
		}
  }-*/;

  native JavaScriptObject update_draw_heatmap_header()/*-{
		$wnd.inchlib_draw_heatmap_header = function() {
			var self = this;
			if (self.settings.heatmap_header && self.header.length > 0) {
				self.header_layer = new Kinetic.Layer();
				var count = self._hack_size(self.leaves_y_coordinates);
				var y = (self.settings.column_dendrogram && self.heatmap_header) ? self.header_height
						+ (self.pixels_for_leaf * count)
						+ 10
						+ self.column_metadata_height
						: self.header_height - 20;
				var rotation = (self.settings.column_dendrogram && self.heatmap_header) ? 90
						: -90;
				var distance_step = 0;
				var x, i, column_header, key;
				var current_headers = [];

				for (i = 0, len = self.on_features["data"].length; i < len; i++) {
					current_headers
							.push(self.header[self.on_features["data"][i]]);
				}

				for (i = 0, len = self.on_features["metadata"].length; i < len; i++) {
					current_headers
							.push(self.header[self.on_features["metadata"][i]
									+ self.dimensions["data"]]);
				}
				if (self.settings.count_column
						&& self.features[self.dimensions["overall"] - 1]) {
					current_headers
							.push(self.header[self.dimensions["overall"] - 1]);
				}
				var max_text_length = self._get_max_length(current_headers);
				var font_size = self._get_font_size(max_text_length,
						self.header_height, self.pixels_for_dimension, 16);
				if (font_size < 8) {
					return;
				}

				for (i = 0, len = current_headers.length; i < len; i++) {
					x = self.heatmap_distance + distance_step
							* self.pixels_for_dimension
							+ self.pixels_for_dimension / 2;
					column_header = self.objects_ref.column_header.clone({
						x : x,
						y : y,
						text : current_headers[i],
						position_index : i,
						fontSize : font_size,
						rotationDeg : rotation,
					});
					self.header_layer.add(column_header);
					distance_step++;
				}

				self.stage.add(self.header_layer);

				if (!(self.settings.dendrogram)) {

					self.header_layer
							.on(
									"click",
									function(evt) {
										var column = evt.target;
										var position_index = column.attrs.position_index;
										for (i = 0; i < self.header_layer
												.getChildren().length; i++) {
											self.header_layer.getChildren()[i]
													.setFill("black");
										}
										evt.target.setAttrs({
											"fill" : "red"
										});
										self._delete_layers([
												self.heatmap_layer,
												self.heatmap_overlay,
												self.highlighted_rows_layer ]);
										self
												._reorder_heatmap(self
														._translate_column_to_feature_index(position_index));
										self._draw_heatmap();
										self.header_layer.draw();
									});

					self.header_layer.on("mouseover", function(evt) {
						var label = evt.target;
						label.setOpacity(0.7);
						this.draw();
					});

					self.header_layer.on("mouseout", function(evt) {
						var label = evt.target;
						label.setOpacity(1);
						this.draw();
					});
				}
			}
		}
  }-*/;

  native JavaScriptObject add_color_scale() /*-{
		$wnd.inchlib.colors["cm.colors"] = {
			"start" : {
				"r" : 128,
				"g" : 255,
				"b" : 255
			},
			"middle" : {
				"r" : 248,
				"g" : 248,
				"b" : 255
			},
			"end" : {
				"r" : 255,
				"g" : 128,
				"b" : 255
			}
		}
		$wnd.inchlib.colors["topo.colors"] = {
			"start" : {
				"r" : 76,
				"g" : 0,
				"b" : 255
			},
			"middle" : {
				"r" : 32,
				"g" : 255,
				"b" : 0
			},
			"end" : {
				"r" : 255,
				"g" : 225,
				"b" : 178
			}
		}
		$wnd.inchlib.colors["terrain.colors"] = {
			"start" : {
				"r" : 0,
				"g" : 167,
				"b" : 0
			},
			"middle" : {
				"r" : 232,
				"g" : 218,
				"b" : 14
			},
			"end" : {
				"r" : 243,
				"g" : 243,
				"b" : 243
			}
		}
		$wnd.inchlib.colors["heat.colors"] = {
			"start" : {
				"r" : 255,
				"g" : 0,
				"b" : 0
			},
			"middle" : {
				"r" : 255,
				"g" : 174,
				"b" : 0
			},
			"end" : {
				"r" : 255,
				"g" : 255,
				"b" : 224
			}
		}
		$wnd.inchlib.colors["rainbow"] = {
			"start" : {
				"r" : 51,
				"g" : 0,
				"b" : 255
			},
			"middle" : {
				"r" : 255,
				"g" : 0,
				"b" : 152
			},
			"end" : {
				"r" : 255,
				"g" : 154,
				"b" : 0
			}
		}
  }-*/;
}
