var InCHlibEx;

(function ($) {

  InCHlibEx = function (settings) {
    var self = this;
    InCHlib.call(self, settings);

    self.colors = self.extendColors(self.colors);
    self.objects_ref = self.extendObjectsRef(self.objects_ref);

    self.cluster_colors = [
      "#001f3f", // NAVY
      "#0074D9", // BLUE
      "#7FDBFF", // AQUA
      "#39CCCC", // TEAL
      "#3D9970", // OLIVE
      "#2ECC40", // GREEN
      "#01FF70", // LIME
      "#FFDC00", // YELLOW
      "#FF851B", // ORANGE
      "#FF4136", // RED
      "#85144b", // MAROON
      "#F012BE", // FUCHSIA
      "#B10DC9", // PURPLE
      "#111111", // BLACK
      "#AAAAAA", // GRAY
      "#DDDDDD"  // SILVER
    ];
  };

  InCHlibEx.prototype = new InCHlib({});

  InCHlibEx.prototype.extendColors = function (colors) {
    colors["cm.colors"] = {
      "start": {"r": 128, "g": 255, "b": 255},
      "middle": {"r": 248, "g": 248, "b": 255},
      "end": {"r": 255, "g": 128, "b": 255}
    };
    colors["topo.colors"] = {
      "start": {"r": 76, "g": 0, "b": 255},
      "middle": {"r": 32, "g": 255, "b": 0},
      "end": {"r": 255, "g": 225, "b": 178}
    };
    colors["terrain.colors"] = {
      "start": {"r": 0, "g": 167, "b": 0},
      "middle": {"r": 232, "g": 218, "b": 14},
      "end": {"r": 243, "g": 243, "b": 243}
    };
    colors["heat.colors"] = {
      "start": {"r": 255, "g": 0, "b": 0},
      "middle": {"r": 255, "g": 174, "b": 0},
      "end": {"r": 255, "g": 255, "b": 224}
    };
    colors["rainbow"] = {
      "start": {"r": 51, "g": 0, "b": 255},
      "middle": {"r": 255, "g": 0, "b": 152},
      "end": {"r": 255, "g": 154, "b": 0}
    };
    return colors;
  };

  InCHlibEx.prototype.extendObjectsRef = function (objects_ref) {
    objects_ref["distance_rect"] = new Kinetic.Rect({
      //fill: "blue",
      lineCap: 'butt',
      value: false,
      stroke: "blue",
      strokeWidth: 100,
      //opacity: 0,
    });
    objects_ref["cutoff_line"] = new Kinetic.Line({
      stroke: "#666",
      strokeWidth: 2,
      dash: [6, 2]
    });
    return objects_ref;
  };

  InCHlibEx.prototype.read_data_from_file = function (json) {
    var self = this;

    var d = new $.Deferred;
    self.loading = d.promise();

    $.ajax({
      url: json,
      type: 'GET',
      dataType: 'json',
      timeout: 30000,
      async: true // Synchronous XMLHttpRequest is deprecated
    }).done(function (json_file) {
      self.read_data(json_file);
      d.resolve();
    }).fail(function () {
      d.reject();
    });
  }

  InCHlibEx.prototype.draw = function () {
    var self = this;

    self.loading.done(function () {
      InCHlib.prototype.draw.call(self);
      self.selection_y = []; // TODO use highlighted_rows_y instead

      self.mouse_down = null;
      self.mouse_down_from = null;
      self.mouse_down_to = null;
      self.selection = null; // TODO use highlighted_rows_y instead
    }).fail(function () {
      alert("Timeout while loading data.")
    });
  }

  InCHlibEx.prototype._draw_color_scale = function () {
    var self = this;
    InCHlib.prototype._draw_color_scale.call(self);

    var color_scale_up = new Kinetic.Text({
      x: 100,
      y: 80,
      text: "+",
      fontSize: 12,
      fontFamily: self.settings.font,
      fontStyle: 'bold',
      fill: 'black',
      align: 'left',
      listening: false
    });
    color_scale_up.setX(color_scale_up.getX() - color_scale_up.getWidth());
    color_scale_up.setY(color_scale_up.getY() - color_scale_up.getHeight());

    var color_scale_down = new Kinetic.Text({
      x: 0,
      y: 80,
      text: "-",
      fontSize: 12,
      fontFamily: self.settings.font,
      fontStyle: 'bold',
      fill: 'black',
      align: 'right',
      listening: false
    });
    color_scale_down.setY(color_scale_down.getY() - color_scale_down.getHeight());

    self.navigation_layer.add(color_scale_down, color_scale_up);
  };

  InCHlibEx.prototype._draw_heatmap = function () {
    var self = this;
    InCHlib.prototype._draw_heatmap.call(self);

    if (!self.settings.heatmap) {
      return;
    }

    // Disable super class's handler
    self.heatmap_layer.off("mouseleave");
    self.heatmap_layer.on("mouseleave", function (evt) {
      self.last_header = null;
      self.heatmap_overlay.destroyChildren();
      self.heatmap_overlay.draw();
      // Added
      self._selection_end(null);
      self.events.heatmap_onmouseout(evt);
    });

    self.cutoff_tool_assist_overlay = new Kinetic.Layer();
    self.cutoff_group_layer = new Kinetic.Layer();
    self.stage.add(self.cutoff_tool_assist_overlay, self.cutoff_group_layer);

  };

  InCHlibEx.prototype._draw_dendrogram_layers = function () {
    var self = this;
    InCHlib.prototype._draw_dendrogram_layers.call(self);

    // Disable super class's handler
    self.cluster_layer.off("click");
    self.cluster_layer.on("click", function (evt) {
      self.unhighlight_cluster();
      self.unhighlight_column_cluster();
      self.unhighlight_selection();
      self.unhighlight_cutoff_selection();
      self.change_selection_mode(null);
      self.events.empty_space_onclick(evt);
    });
  };

  InCHlibEx.prototype._draw_stage_layer = function () {
    var self = this;
    InCHlib.prototype._draw_stage_layer.call(self);

    // Disable super class's handler
    self.stage_layer.off("click");
    self.stage_layer.on("click", function (evt) {
      self.unhighlight_cluster();
      self.unhighlight_column_cluster();
      self.unhighlight_selection();
      self.unhighlight_cutoff_selection();
      self.change_selection_mode(null);
      self.events.empty_space_onclick(evt);
    });
  };

  InCHlibEx.prototype._dendrogram_layers_click = function (layer, evt) {
    var self = this;
    InCHlib.prototype._dendrogram_layers_click.call(self, layer, evt);

    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();
    self.change_selection_mode("dendrogram");
  };

  InCHlibEx.prototype._column_dendrogram_layers_click = function (layer, evt) {
    var self = this;
    InCHlib.prototype._column_dendrogram_layers_click.call(self, layer, evt);

    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();
    self.change_selection_mode(null);
  };

  InCHlibEx.prototype._dendrogram_layers_mousedown = function (layer, evt) {
    var self = this;

    var node_id = evt.target.attrs.path_id;
    clearTimeout(self.timer);
    self.timer = setTimeout(function () {
      self.unhighlight_selection();
      self.unhighlight_cutoff_selection();
      self._get_object_ids(node_id);
      self._zoom_cluster(node_id);
    }, 500);
  };

  InCHlibEx.prototype._column_dendrogram_layers_mousedown = function (layer, evt) {
    var self = this;

    var node_id = evt.target.attrs.path_id;
    clearTimeout(self.timer);
    self.timer = setTimeout(function () {
      self.unhighlight_selection();
      self.unhighlight_cutoff_selection();
      self._get_column_ids(node_id);
      self._zoom_column_cluster(node_id);
    }, 500);
  };

  InCHlibEx.prototype._zoom_cluster = function (node_id) {
    var self = this;
    InCHlib.prototype._zoom_cluster.call(self, node_id);

    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();
    self.change_selection_mode(null);
  }

  InCHlibEx.prototype._unzoom_cluster = function () {
    var self = this;
    InCHlib.prototype._unzoom_cluster.call(self);

    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();
    self.change_selection_mode(null);
  }

  InCHlibEx.prototype._zoom_column_cluster = function (node_id) {
    var self = this;
    InCHlib.prototype._zoom_column_cluster.call(self, node_id);

    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();
    self.change_selection_mode(null);
  }

  InCHlibEx.prototype._unzoom_column_cluster = function () {
    var self = this;
    InCHlib.prototype._unzoom_column_cluster.call(self);

    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();
    self.change_selection_mode(null);
  }

  // To draw cells whose value is null
  InCHlibEx.prototype._draw_heatmap_row = function (node_id, x1, y1) {
    var self = this;

    var node = self.data.nodes[node_id];
    var row = new Kinetic.Group({id: node_id});
    var x2, y2, color, line, value, text, text_value, col_index;

    for (var i = 0, len = self.on_features["data"].length; i < len; i++) {
      col_index = self.on_features["data"][i];
      x2 = x1 + self.pixels_for_dimension;
      y2 = y1;
      value = node.features[col_index];
      text_value = value;

      if (self.settings.alternative_data) {
        text_value = self.alternative_data[node_id][col_index];

        if (self.settings.images_as_alternative_data && text_value !== undefined && text_value !== null && text_value != "") {
          value = null;
          var filepath = self.settings.images_path.dir + text_value + self.settings.images_path.ext;
          filepath = escape(filepath);


          if (self.path2image[text_value] === undefined) {
            var image_obj = new Image();
            image_obj.src = filepath;

            image_obj.onload = function () {
              self.image_counter++;

              if (self.image_counter === Object.keys(self.path2image).length) {
                self.heatmap_layer.draw();
              }

            };

            self.path2image_obj[text_value] = image_obj;
            self.path2image[text_value] = self.objects_ref.image.clone({image: self.path2image_obj[text_value]});
          }

          var image = self.path2image[text_value].clone({
            width: self.pixels_for_dimension,
            height: self.pixels_for_leaf,
            x: x1,
            y: y1 - self._hack_round(0.5 * self.pixels_for_leaf),
            points: [x1, y1, x1 + self.pixels_for_dimension, null],
            column: ["d", col_index].join("_"),
            value: text_value
          });
          row.add(image);
        }
      }

      // From here
      if (!self.settings.images_as_alternative_data) {
        if (value !== null) {
          color = self._get_color_for_value(value, self.data_descs[col_index]["min"], self.data_descs[col_index]["max"], self.data_descs[col_index]["middle"], self.settings.heatmap_colors);
        } else {
          color = "#FFF";
          text_value = "(absent)";
        }

        line = self.objects_ref.heatmap_line.clone({
          stroke: color,
          points: [x1, y1, x2, y2],
          value: text_value,
          column: ["d", col_index].join("_"),
          strokeWidth: self.pixels_for_leaf,
        });
        row.add(line);

        if (self.current_draw_values) {
          text = self.objects_ref.heatmap_value.clone({
            x: self._hack_round((x1 + x2) / 2 - ("" + text_value).length * (self.value_font_size / 4)),
            y: self._hack_round(y1 - self.value_font_size / 2),
            fontSize: self.value_font_size,
            text: text_value,
          });
          row.add(text);
        }
      }
      // Up to here

      x1 = x2;
    }

    if (self.settings.metadata) {
      var metadata = self.metadata.nodes[node_id];

      if (metadata !== undefined) {
        for (var i = 0, len = self.on_features["metadata"].length; i < len; i++) {
          col_index = self.on_features["metadata"][i];
          value = metadata[col_index];
          x2 = x1 + self.pixels_for_dimension;
          y2 = y1;

          if (value !== null && value !== undefined) {
            text_value = value;

            if (self.metadata_descs[col_index]["str2num"] !== undefined) {
              value = self.metadata_descs[col_index]["str2num"][value];
            }
            color = self._get_color_for_value(value, self.metadata_descs[col_index]["min"], self.metadata_descs[col_index]["max"], self.metadata_descs[col_index]["middle"], self.settings.metadata_colors);

            line = self.objects_ref.heatmap_line.clone({
              stroke: color,
              points: [x1, y1, x2, y2],
              value: text_value,
              column: ["m", col_index].join("_"),
              strokeWidth: self.pixels_for_leaf,
            });
            row.add(line);

            if (self.current_draw_values) {
              text = self.objects_ref.heatmap_value.clone({
                text: text_value,
                fontSize: self.value_font_size,
              });

              width = text.getWidth();
              x = self._hack_round((x1 + x2) / 2 - width / 2);
              y = self._hack_round(y1 - self.value_font_size / 2);
              text.position({x: x, y: y});
              row.add(text);
            }
          }
          x1 = x2;
        }
      }
    }

    if (self.settings.count_column && self.features[self.dimensions["overall"] - 1]) {
      x2 = x1 + self.pixels_for_dimension;
      var count = node.objects.length;
      color = self._get_color_for_value(count, self.min_item_count, self.max_item_count, self.middle_item_count, self.settings.count_column_colors);

      line = self.objects_ref.heatmap_line.clone({
        stroke: color,
        points: [x1, y1, x2, y2],
        value: count,
        column: "Count",
        strokeWidth: self.pixels_for_leaf,
      });
      row.add(line);

      if (self.current_draw_values) {
        text = self.objects_ref.heatmap_value.clone({
          text: count,
        });

        width = text.getWidth();
        x = self._hack_round((x1 + x2) / 2 - width / 2);
        y = self._hack_round(y1 - self.value_font_size / 2);
        text.position({x: x, y: y});
        row.add(text);
      }
    }
    return row;
  }; // _draw_heatmap_row

  InCHlibEx.prototype._draw_row_ids = function () {
    var self = this;
    InCHlib.prototype._draw_row_ids.call(self);

    if (self.pixels_for_leaf < 6 || self.row_id_size < 5) {
      return;
    }

    var i, objects, keys, len;
    self.row_id_in_order = [];
    self.row_y_in_order = [];

    for (i = 0, keys = Object.keys(self.leaves_y_coordinates), len = keys.length; i < len; i++) {
      var leaf_id = keys[i];
      objects = self.data.nodes[leaf_id].objects;
      if (objects.length > 1) {
        return;
      }
      self.row_id_in_order.push(objects[0]);
      self.row_y_in_order.push(self.leaves_y_coordinates[leaf_id]);
    }
  };

  InCHlibEx.prototype._bind_row_events = function (row) {
    var self = this;
    InCHlib.prototype._bind_row_events.call(self, row);

    row.on("mousedown", function (evt) {
      console.log("mousedown")
      self._selection_start(evt);
    });

    row.on("mouseup", function (evt) {
      self._selection_end(evt);
    });

    // Disable super class's event
    row.off("mouseover");
    row.on("mouseover", function (evt) {
      self._draw_col_label(evt);
      if (self.mouse_down === true) {
        var row_id = evt.target.parent.attrs.id;

        if (self.last_row == row_id) {
          return;
        }
        self.mouse_down_to = self.data.nodes[row_id].objects[0];

        var from = self.row_id_in_order.indexOf(self.mouse_down_from);
        var to = self.row_id_in_order.indexOf(self.mouse_down_to);
        self.selection = self.row_id_in_order.slice(Math.min(from, to), Math.max(from, to) + 1);
        self.selection_y = self.row_y_in_order.slice(Math.min(from, to), Math.max(from, to) + 1);

        self.highlight_rows(self.selection);

        self.last_row = row_id;
      }
    });
  };

  InCHlibEx.prototype._selection_start = function (evt) {
    var self = this;

    self.mouse_down = true;
    self.unhighlight_selection();
    self.unhighlight_cutoff_selection();

    var row_id = evt.target.parent.attrs.id;
    self.mouse_down_from = self.data.nodes[row_id].objects[0];
    //$wnd.widget.@otgviewer.client.HeatmapDialog::updateSaveButton(Ljava/lang/String;)("F");
  };

  InCHlibEx.prototype._selection_end = function (evt) {
    var self = this;

    if (self.mouse_down != true) {
      return;
    }

    self.mouse_down = false;
    if (self.selection) {
      if (self.selection.length == 0) {
        self.unhighlight_rows();
        self.unhighlight_cutoff_selection();
        //$wnd.widget.@otgviewer.client.HeatmapDialog::updateSaveButton(Ljava/lang/String;)("F");
      } else {
        self.change_selection_mode("free");
        self._draw_selection_layer(self.selection);
        //$wnd.widget.@otgviewer.client.HeatmapDialog::updateSaveButton(Ljava/lang/String;)("T");
      }
    }
  };

  InCHlibEx.prototype._draw_selection_layer = function (selection) {
    var self = this;

    self.row_selection_group = new Kinetic.Group();
    var visible = self._get_visible_count();
    var count = selection.length;
    var x = self.distance - 30;
    var y = self.header_height + self.column_metadata_height - 40;

    x = self.distance + self.dendrogram_heatmap_distance;
    var width = visible * self.pixels_for_dimension + self.heatmap_distance;
    var upper_y = self.selection_y[0] - self.pixels_for_leaf / 2;
    var lower_y = self.selection_y[self.selection_y.length - 1] + self.pixels_for_leaf / 2;

    var cluster_overlay_1 = self.objects_ref.cluster_overlay.clone({
      x: x,
      y: self.header_height + self.column_metadata_height + 5,
      width: width,
      height: self._hack_round(upper_y - self.header_height - self.column_metadata_height - 5),
    });

    var cluster_border_1 = self.objects_ref.cluster_border.clone({
      points: [0, upper_y, width, upper_y],
    });

    var cluster_overlay_2 = self.objects_ref.cluster_overlay.clone({
      x: x,
      y: lower_y,
      width: width,
      height: self.settings.height - lower_y - self.footer_height + 5,
    });

    var cluster_border_2 = self.objects_ref.cluster_border.clone({
      points: [0, lower_y, width, lower_y],
    });

    self.row_selection_group.add(cluster_overlay_1, cluster_overlay_2, cluster_border_1, cluster_border_2);
    self.cluster_layer.add(self.row_selection_group);
    self.stage.add(self.cluster_layer);

    self.row_selection_group.on("mousedown", function (evt) {
      self.unhighlight_selection();
    });

    self.cluster_layer.draw();
    self.navigation_layer.moveToTop();
  };

  InCHlibEx.prototype.unhighlight_selection = function () {
    var self = this;

    if (self.selection) {
      self.unhighlight_rows();
      self.row_selection_group.destroy();
      self.dendrogram_layer.draw();
      self.cluster_layer.draw();
      self.selection_y = [];
      self.current_object_ids = [];
      self.mouse_down = null;
      self.mouse_down_from = null;
      self.mouse_down_to = null;
      self.selection = null;
      self.change_selection_mode(null);
    }
  };

  InCHlibEx.prototype._dendrogram_layers_mouseover = function (layer, evt) {
    var self = this;

    if (evt.target.attrs.id) {
      self.path_overlay = evt.target.attrs.path.clone({"strokeWidth": 4});
      self.dendrogram_hover_layer.add(self.path_overlay);
      self.dendrogram_hover_layer.draw();
    }
  };

  InCHlibEx.prototype._draw_column_cluster = function (node_id) {
    var self = this;

    self.columns_start_index = self.current_column_ids[0];
    self.on_features["data"] = self.current_column_ids;
    var distance = self.distance;
    self._adjust_horizontal_sizes();
    self._delete_layers([self.column_dendrogram_layer, self.heatmap_layer, self.heatmap_overlay, self.column_cluster_group, self.navigation_layer, self.highlighted_rows_layer], [self.dendrogram_hover_layer]);
    if (self.settings.heatmap_header) {
      self._delete_layers([self.header_layer]);
    }
    self._draw_column_dendrogram(node_id);
    self._draw_heatmap();
    self._draw_heatmap_header();
    self._draw_navigation();

    if (distance !== self.distance) {
      self._delete_layers([self.dendrogram_layer, self.cluster_layer]);
      var row_node = (self.zoomed_clusters["row"].length > 0) ? self.zoomed_clusters["row"][self.zoomed_clusters["row"].length - 1] : self.root_id;
      self._draw_row_dendrogram(row_node);
      if (self.last_highlighted_cluster !== null) {
        self._highlight_path(self.last_highlighted_cluster, "#F5273C");
        self.dendrogram_layer.draw();
        self._draw_cluster_layer(self.last_highlighted_cluster);
      }
    }
    else {
      self.cluster_layer.moveToTop();
      self.cluster_layer.draw();
    }
  };

  InCHlibEx.prototype._draw_row_dendrogram = function (node_id) {
    var self = this;

    self.cutoff_tool_layer = new Kinetic.Layer();
    self.stage.add(self.cutoff_tool_layer);

    InCHlib.prototype._draw_row_dendrogram.call(self, node_id);

    self.cutoff_tool_layer.draw();
  };

  InCHlibEx.prototype._draw_distance_scale = function (distance) {
    var self = this;
    InCHlib.prototype._draw_distance_scale.call(self, distance);

    if (!self.settings.navigation_toggle.distance_scale) {
      return;
    }
    var y1 = self.header_height + self.column_metadata_height + self.settings.column_metadata_row_height / 2 - 10;
    var y2 = y1;
    var x1 = 0;
    var x2 = self.distance;

    var distance = Math.round(100 * self.distance / self.distance_step) / 100;

    var cutoff_assist_rect = new Kinetic.Line({
      points: [x1, y1, x2, y2],
      stroke: "white",
      strokeWidth: 15,
      opacity: 0,
    });
    self.cutoff_tool_layer.add(cutoff_assist_rect);

    cutoff_assist_rect.on("mouseenter", function (evt) {
      var x = evt.evt["layerX"];
      var value = (distance > 0) ? Math.round(100 * (x2 - x) / x2 * distance) / 100 : 0;
      self._cutoff_assist_rect_mouseenter(evt, x, y1 - 10, value);
    });
    cutoff_assist_rect.on("mousemove", function (evt) {
      var x = evt.evt["layerX"];
      var value = (distance > 0) ? Math.round(100 * (x2 - x) / x2 * distance) / 100 : 0;
      self._cutoff_assist_rect_mousemove(evt, x, y1 - 10, value);
    });
    cutoff_assist_rect.on("mouseleave", function (evt) {
      self._cutoff_assist_rect_mouseleave(evt);
    });
    cutoff_assist_rect.on("click", function (evt) {
      self.unhighlight_cluster();
      self.unhighlight_column_cluster();
      self.unhighlight_selection();
      self.change_selection_mode("cutoff");

      var x = evt.evt["layerX"];
      var value = (distance > 0) ? Math.round(100 * (x2 - x) / x2 * distance) / 100 : 0;
      self.highlight_cutoff_selection(value);

      var y1 = self.header_height;
      var y2 = self.header_height + self.column_metadata_height + (self.heatmap_array.length + 0.5) * self.pixels_for_leaf;
      var cutoff_line = self.objects_ref.cutoff_line.clone({points: [0, y1, 0, y2], stroke: "#F00"}).x(x);

      self.cutoff_group_layer.add(cutoff_line);
      self.cutoff_group_layer.draw();
    })
  };

  InCHlibEx.prototype._cutoff_assist_rect_mouseenter = function (evt, x, y, value) {
    var self = this;

    self.cutoff_tool_assist_overlay.destroyChildren();
    self.cutoff_distance_tooltip = self.objects_ref.tooltip_label.clone({x: x, y: y, id: "cotoff_tooltip_label"});
    self.cutoff_distance_tooltip.add(self.objects_ref.tooltip_tag.clone({pointerDirection: 'down'}));
    self.cutoff_distance_tooltip.add(self.objects_ref.tooltip_text.clone({text: value, id: "cutoff_tooltip_tag"}));

    var y1 = self.header_height;
    var y2 = self.header_height + self.column_metadata_height + (self.heatmap_array.length + 0.5) * self.pixels_for_leaf;
    var cutoff_line = self.objects_ref.cutoff_line.clone({points: [0, y1, 0, y2], id: "cutoff_line"}).x(x);
    self.cutoff_tool_assist_overlay.add(cutoff_line);
    self.cutoff_tool_assist_overlay.add(self.cutoff_distance_tooltip);
    self.cutoff_tool_assist_overlay.moveToTop();

    self.cutoff_tool_assist_overlay.draw();
  };

  InCHlibEx.prototype._cutoff_assist_rect_mousemove = function (evt, x, y, value) {
    var self = this;

    if (self.cutoff_distance_tooltip) {
      var minX = self.cutoff_distance_tooltip.width() / 2.0;
      var x1 = (x < minX) ? minX : x;
      self.cutoff_tool_assist_overlay.find("#cotoff_tooltip_label")[0].x(x1);
      self.cutoff_tool_assist_overlay.find("#cutoff_tooltip_tag")[0].text(value);
      self.cutoff_tool_assist_overlay.find("#cutoff_line")[0].x(x);
    }

    self.cutoff_tool_assist_overlay.draw();
  };

  InCHlibEx.prototype._cutoff_assist_rect_mouseleave = function (evt) {
    var self = this;

    self.cutoff_distance_tooltip = null;
    self.cutoff_tool_assist_overlay.destroyChildren();

    self.cutoff_tool_assist_overlay.draw();
  };

  InCHlibEx.prototype.highlight_cutoff_selection = function (distance) {
    var self = this;

    self.unhighlight_cutoff_selection();

    self.cutoff_distance = distance;

    var custer_groups = {};
    var nodes = self.data.nodes;
    var tmp = {};
    var key;
    for (key in nodes) {
      var d = nodes[key]["distance"];
      if (d == null || distance < d) {
        continue;
      }
      tmp[key] = nodes[key];
    }

    for (key in tmp) {
      if (tmp[key]["left_child"] || tmp[key]["right_child"]) {
        continue;
      }

      var child = key;
      var parent = tmp[key]["parent"];
      while (parent in tmp) {
        child = parent;
        parent = tmp[parent]["parent"];
      }

      var group_id = child;
      if (!custer_groups[group_id]) {
        custer_groups[group_id] = [];
      }
      custer_groups[group_id].push(tmp[key]["objects"][0]);
    }

    var toArray = function (hash) {
      var ret = [];
      for (key in hash) {
        ret.push(hash[key]);
      }
      return ret;
    };

    self._draw_cutoff_group_layer(toArray(custer_groups));
  };

  InCHlibEx.prototype.unhighlight_cutoff_selection = function () {
    var self = this;
    if (self.cutoff_distance) {
      self.cutoff_group_layer.destroyChildren();
      self.cutoff_group_layer.draw();
      self.cutoff_distance = null;
      self.change_selection_mode(null);
    }
  };

  InCHlibEx.prototype._draw_cutoff_group_layer = function (custer_groups) {
    var self = this;

    self.cutoff_group = new Kinetic.Group();

    var x = self.distance + self.dendrogram_heatmap_distance;
    var y = self.header_height + self.column_metadata_height - 40;
    var visible = self._get_visible_count();
    var width = visible * self.pixels_for_dimension + self.heatmap_distance;

    var cluster_overlays = [];
    var cluster_borders = [];
    {
      var upper_y = self.row_y_in_order[0] - self.pixels_for_leaf / 2;
      cluster_borders.push(self.objects_ref.cluster_border.clone({points: [0, upper_y, width, upper_y]}));
    }

    custer_groups.forEach(function (element, index, array) {
      if (element.length == 0) {
        return;
      }
      var min = Number.MAX_VALUE;
      var max = 0;
      element.forEach(function (element, index, array) {
        var i = self.row_id_in_order.indexOf(element);
        min = (i < min) ? i : min;
        max = (max < i) ? i : max;
      });
      var upper_y = self.row_y_in_order[min] - self.pixels_for_leaf / 2.0;
      var lower_y = self.row_y_in_order[max] + self.pixels_for_leaf / 2.0;

      cluster_overlays.push(self.objects_ref.cluster_overlay.clone({
        x: x,
        y: upper_y,
        width: width,
        height: self._hack_round(lower_y - upper_y),
        fill: self.cluster_colors[index % self.cluster_colors.length],
        opacity: 0.2,
      }));
      cluster_borders.push(self.objects_ref.cluster_border.clone({points: [0, lower_y, width, lower_y]}));
    });

    cluster_overlays.forEach(function (element, index, arrar) {
      self.cutoff_group.add(element);
    });
    cluster_borders.forEach(function (element, index, arrar) {
      self.cutoff_group.add(element);
    });

    self.cutoff_group_layer.add(self.cutoff_group);

    self.cutoff_group_layer.moveToTop();
  };

  InCHlibEx.prototype.change_selection_mode = function (selection_mode) {
    var self = this;

    self.selection_mode = selection_mode;
    self.selection_mode_changed();
  };

  InCHlibEx.prototype.selection_mode_changed = function () {
    // TODO override in GWT
  }

})(jQuery);
