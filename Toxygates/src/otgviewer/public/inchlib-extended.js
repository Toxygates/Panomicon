var InCHlibEx;

(function ($) {

  InCHlibEx = function (settings) {
    var self = this;
    InCHlib.call(self, settings);

    self.colors = self._extendColors(self.colors);
    self.objects_ref = self._extendObjectsRef(self.objects_ref);

    self.cluster_colors = [
      '#001f3f', // NAVY
      '#0074D9', // BLUE
      '#7FDBFF', // AQUA
      '#39CCCC', // TEAL
      '#3D9970', // OLIVE
      '#2ECC40', // GREEN
      '#01FF70', // LIME
      '#FFDC00', // YELLOW
      '#FF851B', // ORANGE
      '#FF4136', // RED
      '#85144b', // MAROON
      '#F012BE', // FUCHSIA
      '#B10DC9', // PURPLE
      '#111111', // BLACK
      '#AAAAAA', // GRAY
      '#DDDDDD'  // SILVER
    ];

    self.selection_state = {
      none: 'none',
      range: 'range',
      dendrogram: 'dendrogram',
      cutoff: 'cutoff'
    };

    if (!('log_axis' in self.settings)) {
      self.settings['log_axis'] = false;
    }
  };

  InCHlibEx.prototype = new InCHlib({});

  InCHlibEx.prototype._extendColors = function (colors) {
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

  InCHlibEx.prototype._extendObjectsRef = function (objects_ref) {
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

  InCHlibEx.prototype._change_selection_state = function (state) {
    var self = this;

    if (!state) {
      state = self.selection_state.none;
    }

    self.current_selection_state = state;
    self.selection_state_changed(state);
  };

  InCHlibEx.prototype.selection_state_changed = function (state) {
  };

  /** @override */
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
  };

  /** @override */
  InCHlibEx.prototype.read_data = function (json) {
    var self = this;

    var d = new $.Deferred;
    self.loading = d.promise();

    InCHlib.prototype.read_data.call(this, json);

    d.resolve();
  };

  /** @override */
  InCHlibEx.prototype.draw = function () {
    var self = this;

    self.loading.done(function () {
      self.minDistance = Math.min(Number.MAX_VALUE);
      self.maxDistance = -Math.min(Number.MAX_VALUE);

      InCHlib.prototype.draw.call(self);

      self.last_highlighted_range = null
      self.last_highlighted_cutoff_cluster = null
      self.mouse_down = null;
      self.mouse_down_from = null;
      self.mouse_down_to = null;

      self._change_selection_state(null);
    }).fail(function () {
      alert("Fail to load data.")
    });
  };

  /** @override */
  InCHlibEx.prototype._preprocess_heatmap_data = function () {
    var self = this;
    var heatmap_array = [], i, j = 0, keys, key, len, data, node;

    for (i = 0, keys = Object.keys(self.data.nodes), len = keys.length; i < len; i++) {
      key = keys[i];
      node = self.data.nodes[key];

      if (node.count == 1) {
        data = node.features;
        heatmap_array.push([key]);
        heatmap_array[j].push.apply(heatmap_array[j], data);
        if (self.settings.metadata) {
          heatmap_array[j].push.apply(heatmap_array[j], self.metadata.nodes[key]);
        }
        j++;
      } else {
    	if (node.distance > 0) {
          self.minDistance = Math.min(self.minDistance, node.distance);
    	}
      }
      self.maxDistance = Math.max(self.maxDistance, node.distance);
    }

    return heatmap_array;
  };

  /** @override */
  InCHlibEx.prototype._draw_color_scale = function () {
    var self = this;
    InCHlib.prototype._draw_color_scale.call(self);

    var color_scale_up = new Kinetic.Text({
      x: 100,
      y: 80,
      text: '+',
      fontSize: 14,
      fontFamily: self.settings.font,
      fontStyle: 'bold',
      fill: 'black',
      align: 'left',
      listening: false
    });
    color_scale_up.x(color_scale_up.x() - color_scale_up.width());
    color_scale_up.y(color_scale_up.y() - color_scale_up.height());

    var color_scale_down = new Kinetic.Text({
      x: 0,
      y: 80,
      text: '-',
      fontSize: 14,
      fontFamily: self.settings.font,
      fontStyle: 'bold',
      fill: 'black',
      align: 'right',
      listening: false
    });
    color_scale_down.y(color_scale_down.y() - color_scale_down.height());

    self.navigation_layer.add(color_scale_down, color_scale_up);
  };

  /** @override */
  InCHlibEx.prototype._draw_heatmap = function () {
    var self = this;
    InCHlib.prototype._draw_heatmap.call(self);

    // Disable the handler set in super class
    self.heatmap_layer.off("mouseleave");
    self.heatmap_layer.on("mouseleave", function (evt) {
      //self.last_header = null; // Defined in super class, but not used.
      self.heatmap_overlay.destroyChildren();
      self.heatmap_overlay.draw();
      self.events.heatmap_onmouseout(evt);

      if (self.mouse_down) {
        self._stop_drugging(evt);
      }
    });
  };

  /** @override */
  InCHlibEx.prototype._draw_dendrogram_layers = function () {
    var self = this;
    InCHlib.prototype._draw_dendrogram_layers.call(self);

    // Disable the handler set in super class
    self.cluster_layer.off("click");
    self.cluster_layer.on("click", function (evt) {
      self._reset_all_highlight();
      self.events.empty_space_onclick(evt);
    });
  };

  /** @override */
  InCHlibEx.prototype._draw_stage_layer = function () {
    var self = this;
    InCHlib.prototype._draw_stage_layer.call(self);

    // Disable super class's handler
    self.stage_layer.off("click");
    self.stage_layer.on("click", function (evt) {
      self._reset_all_highlight();
      self.events.empty_space_onclick(evt);
    });
  };

  /** @override */
  InCHlibEx.prototype._dendrogram_layers_click = function (layer, evt) {
    var self = this;

    self._reset_all_highlight();

    InCHlib.prototype._dendrogram_layers_click.call(self, layer, evt);

    self._change_selection_state(self.selection_state.dendrogram);

    var path_id = evt.target.attrs.path_id;
    // console.log(path_id + ": d = " + self.data.nodes[path_id].distance)
  };

  /** @override */
  InCHlibEx.prototype._column_dendrogram_layers_click = function (layer, evt) {
    var self = this;

    self._reset_all_highlight();

    InCHlib.prototype._column_dendrogram_layers_click.call(self, layer, evt);

    self._change_selection_state(null);
  };

  /** @override */
  InCHlibEx.prototype._dendrogram_layers_mousedown = function (layer, evt) {
    var self = this;

    var node_id = evt.target.attrs.path_id;
    clearTimeout(self.timer);
    self.timer = setTimeout(function () {
      self._reset_all_highlight();
      self._get_object_ids(node_id);
      self._zoom_cluster(node_id);
    }, 500);
  };

  /** @override */
  InCHlibEx.prototype._column_dendrogram_layers_mousedown = function (layer, evt) {
    var self = this;

    var node_id = evt.target.attrs.path_id;
    clearTimeout(self.timer);
    self.timer = setTimeout(function () {
      self._reset_all_highlight();
      self._get_column_ids(node_id);
      self._zoom_column_cluster(node_id);
    }, 500);
  };

  /** @override */
  InCHlibEx.prototype._zoom_cluster = function (node_id) {
    var self = this;
    InCHlib.prototype._zoom_cluster.call(self, node_id);

    self._reset_all_highlight();
  }

  /** @override */
  InCHlibEx.prototype._unzoom_cluster = function () {
    var self = this;
    InCHlib.prototype._unzoom_cluster.call(self);

    self._reset_all_highlight();
  }

  /** @override */
  InCHlibEx.prototype._zoom_column_cluster = function (node_id) {
    var self = this;
    InCHlib.prototype._zoom_column_cluster.call(self, node_id);

    self._reset_all_highlight();
  }

  /** @override */
  InCHlibEx.prototype._unzoom_column_cluster = function () {
    var self = this;
    InCHlib.prototype._unzoom_column_cluster.call(self);

    self._reset_all_highlight();
  }

  /** @override */
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

      // Changes from here
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
        for (i = 0, len = self.on_features["metadata"].length; i < len; i++) {
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

  /** @override */
  InCHlibEx.prototype._get_row_id_size = function(){
    var self = this;
    var objects, object_y = [], leaf_id, values = [], text;

    for (var i = 0, len = self.heatmap_array.length; i < len; i++) {
      leaf_id = self.heatmap_array[i][0];
      objects = self.data.nodes[leaf_id].objects;
      if(objects.length > 1){
        return;
      }
      values.push(objects[0] + self.data.nodes[leaf_id].appendix);
    }
    var max_length = self._get_max_length(values);
    var test_string = "";
    for (var i = 0; i < max_length; i++) {
      test_string += "E";
    }

    if (self.settings.fixed_row_id_size) {
      var test = new Kinetic.Text({
        fontFamily: self.settings.font,
        fontSize: self.settings.fixed_row_id_size,
        fontStyle: "italic",
        listening: false,
        text: test_string
      });
      self.row_id_size = self.settings.fixed_row_id_size;
      self.right_margin = 20 + test.width();

      if (this.right_margin < 100) {
        self.right_margin = 100;
      }
    } else {
      self.row_id_size = self._get_font_size(max_length, 85, self.pixels_for_leaf, 10);
      self.right_margin = 100;
    }

  };

  /** @override */
  InCHlibEx.prototype._draw_row_ids = function () {
    var self = this;
    if (self.pixels_for_leaf < 6 || self.row_id_size < 5) {
      return;
    }

    var i, objects, keys, len;
    var object_y = [];
    self.row_id_in_order = [];
    self.row_y_in_order = [];

    for (i = 0, keys = Object.keys(self.leaves_y_coordinates), len = keys.length; i < len; i++) {
      var leaf_id = keys[i];
      objects = self.data.nodes[leaf_id].objects;
      if (objects.length > 1) {
        return;
      }
      object_y.push([objects[0], self.leaves_y_coordinates[leaf_id], self.data.nodes[leaf_id].appendix]);

      self.row_id_in_order.push(objects[0]);
      self.row_y_in_order.push(self.leaves_y_coordinates[leaf_id]);
    }

    var x = self.distance + self._get_visible_count()*self.pixels_for_dimension + 15;

    var displayText = function(object_y, i) {
      var text = object_y[i][0];
      if (object_y[i][2]) {
        text = text + '(' + object_y[i][2] + ')';
      }
      return text;
    };
    for (i = 0; i < object_y.length; i++) {
      var text = self.objects_ref.heatmap_value.clone({
        x: x,
        y: self._hack_round(object_y[i][1] - self.row_id_size/2),
        fontSize: self.row_id_size,
        text: displayText(object_y, i),
        fontStyle: 'italic',
        fill: "gray"
      });
      self.heatmap_layer.add(text);
    }

  };

  /** @override */
  InCHlibEx.prototype._bind_row_events = function (row) {
    var self = this;
    InCHlib.prototype._bind_row_events.call(self, row);

    row.on("mousedown", function (evt) {
      self._start_drugging(evt);
    });

    row.on("mouseup", function (evt) {
      self._stop_drugging(evt);
    });

    // Disable super class's event
    row.off("mouseover");
    row.on("mouseover", function (evt) {
      self._draw_col_label(evt);
      if (self.mouse_down === true) {
        self._continue_drugging(evt.target.parent.attrs.id);
      }
    });
  };

  InCHlibEx.prototype._highlight_range = function (from, to) {
    var self = this;

    self.current_object_ids = self.row_id_in_order.slice(Math.min(from, to), Math.max(from, to) + 1);
    self.highlighted_rows_y = self.row_y_in_order.slice(Math.min(from, to), Math.max(from, to) + 1);
    self.last_highlighted_range = self.current_object_ids;

    self.highlight_rows(self.current_object_ids);
  }

  InCHlibEx.prototype._unhighlight_range = function () {
    var self = this;

    if (self.last_highlighted_range) {
      self.unhighlight_rows();

      self.row_range_selection_group.destroy();
      self.cluster_layer.draw();
      self.current_object_ids = [];
      self.highlighted_rows_y = [];
      self.last_highlighted_range = null;
      self.mouse_down = false;
      self.mouse_down_from = null;
      self.mouse_down_to = null;

      self._change_selection_state(null);
    }
  };

  /** @override */
  InCHlibEx.prototype.unhighlight_cluster = function () {
    var self = this;

    var isNotNull = (self.last_highlighted_cluster != null);

    InCHlib.prototype.unhighlight_cluster.call(this);

    if (isNotNull) {
      self._change_selection_state(null);
    }
  }

  InCHlibEx.prototype._reset_all_highlight = function () {
    var self = this;

    self.unhighlight_rows();
    self.unhighlight_cluster();
    self.unhighlight_column_cluster();
    self._unhighlight_range();
    self._unhighlight_cutoff_cluster();
  }

  InCHlibEx.prototype._start_drugging = function (evt) {
    var self = this;

    self._reset_all_highlight();

    self.mouse_down = true;
    self.mouse_down_from = self.data.nodes[evt.target.parent.attrs.id].objects[0];
  };

  InCHlibEx.prototype._continue_drugging = function (row_id) {
    var self = this;
    if (self.last_row == row_id) {
      return;
    }

    self.mouse_down_to = self.data.nodes[row_id].objects[0];

    var from = self.row_id_in_order.indexOf(self.mouse_down_from);
    var to = self.row_id_in_order.indexOf(self.mouse_down_to);

    self._highlight_range(from, to);

    self.last_row = row_id;
  };

  InCHlibEx.prototype._stop_drugging = function (evt) {
    var self = this;

    if (!self.mouse_down) {
      return;
    }

    self.mouse_down = false;
    if (self.current_object_ids.length == 0) {
      self._reset_all_highlight();
    } else {
      self._draw_range_selection_overlay();
      self._change_selection_state(self.selection_state.range);
    }
  };

  InCHlibEx.prototype._draw_range_selection_overlay = function () {
    var self = this;

    self.row_range_selection_group = new Kinetic.Group();
    var visible = self._get_visible_count();
    var x = self.distance - 30;
    var y = self.header_height + self.column_metadata_height - 40;

    x = self.distance + self.dendrogram_heatmap_distance;
    var width = visible * self.pixels_for_dimension + self.heatmap_distance;
    var upper_y = self.highlighted_rows_y[0] - self.pixels_for_leaf / 2;
    var lower_y = self.highlighted_rows_y[self.highlighted_rows_y.length - 1] + self.pixels_for_leaf / 2;

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

    self.row_range_selection_group.add(cluster_overlay_1, cluster_overlay_2, cluster_border_1, cluster_border_2);
    self.cluster_layer.add(self.row_range_selection_group);
    self.stage.add(self.cluster_layer);

    self.row_range_selection_group.on("mousedown", function (evt) {
      self._unhighlight_range();
    });

    self.cluster_layer.draw();
    self.navigation_layer.moveToTop();
  };

  /** @override */
  InCHlibEx.prototype._draw_column_cluster = function (node_id) {
    var self = this;

    self.columns_start_index = self.current_column_ids[0];
    self.on_features["data"] = self.current_column_ids;
    var distance = self.distance;
    self._adjust_horizontal_sizes();
    self._delete_layers([
      self.column_dendrogram_layer,
      self.cutoff_line_layer,
      self.cutoff_cluster_overlay,
      self.heatmap_layer,
      self.heatmap_overlay,
      self.column_cluster_group,
      self.navigation_layer,
      self.highlighted_rows_layer
    ], [self.dendrogram_hover_layer]);
    if (self.settings.heatmap_header) {
      self._delete_layers([self.header_layer]);
    }
    self._draw_column_dendrogram(node_id);
    self._draw_heatmap();
    self._draw_heatmap_header();
    self._draw_navigation();

    if (distance !== self.distance) {
      self._delete_layers([self.scale_mouseover_layer, self.dendrogram_layer, self.cluster_layer]);
      var row_node = (self.zoomed_clusters["row"].length > 0) ? self.zoomed_clusters["row"][self.zoomed_clusters["row"].length - 1] : self.root_id;
      self._draw_row_dendrogram(row_node);
      if (self.last_highlighted_cluster !== null) {
        self._highlight_path(self.last_highlighted_cluster, "#F5273C");
        self.dendrogram_layer.draw();
        self.scale_mouseover_layer.draw();
        self._draw_cluster_layer(self.last_highlighted_cluster);
      }
    }
    else {
      self.cluster_layer.moveToTop();
      self.cluster_layer.draw();
    }
  };

  /** @override */
  InCHlibEx.prototype._draw_cluster = function (node_id) {
    var self = this;

    self._delete_layers([self.scale_mouseover_layer, self.cutoff_line_layer, self.cutoff_cluster_overlay]);

    InCHlib.prototype._draw_cluster.call(this, node_id);
  };

  var log10 = Math.log10 || function (x) {
      return Math.log(x) / Math.LN10;
    };

  /** @override */
  InCHlibEx.prototype._draw_row_dendrogram = function (node_id) {
    var self = this;
    self.dendrogram_layer = new Kinetic.Layer();
    var node = self.data.nodes[node_id];
    var count = node.count;

    self.distance_step = (function () {
      if (self.settings.log_axis) {
        return (self.distance - 5) / (log10(node.distance) - log10(self.minDistance));
      } else {
        return self.distance / node.distance;
      }
    })();

    self.leaves_y_coordinates = {};
    self.objects2leaves = {};

    self._adjust_leaf_size(count);
    self.settings.height = count * self.pixels_for_leaf + self.header_height + self.footer_height + self.column_metadata_height;

    self.stage.setWidth(self.settings.width);
    self.stage.setHeight(self.settings.height);

    var current_left_count = 0;
    var current_right_count = 0;
    var y = self.header_height + self.column_metadata_height + self.pixels_for_leaf / 2;

    if (node.count > 1) {
      current_left_count = self.data.nodes[node.left_child].count;
      current_right_count = self.data.nodes[node.right_child].count;
    }
    self._draw_row_dendrogram_node(node_id, node, current_left_count, current_right_count, 0, y);
    self.middle_item_count = (self.min_item_count + self.max_item_count) / 2;
    self._draw_distance_scale(node.distance);
    self.stage.add(self.dendrogram_layer);

    self._bind_dendrogram_hover_events(self.dendrogram_layer);

    self.dendrogram_layer.on("click", function (evt) {
      self._dendrogram_layers_click(this, evt);
    });

    self.dendrogram_layer.on("mousedown", function (evt) {
      self._dendrogram_layers_mousedown(this, evt);
    });

    self.dendrogram_layer.on("mouseup", function (evt) {
      self._dendrogram_layers_mouseup(this, evt);
    });

    self.stage.add(self.scale_mouseover_layer);

    // initialise layers for cutoff specification
    self.cutoff_line_layer = new Kinetic.Layer();
    self.cutoff_cluster_overlay = new Kinetic.Layer();
    self.stage.add(self.cutoff_line_layer, self.cutoff_cluster_overlay);

    self.scale_mouseover_layer.draw();
  };

  /** @override */
  InCHlibEx.prototype._draw_row_dendrogram_node = function (node_id, node, current_left_count, current_right_count, x, y) {
    var self = this;

    if (node.count != 1) {
      var node_neighbourhood = self._get_node_neighbourhood(node, self.data.nodes);
      var right_child = self.data.nodes[node.right_child];
      var left_child = self.data.nodes[node.left_child];
      var y1 = self._get_y1(node_neighbourhood, current_left_count, current_right_count);
      var y2 = self._get_y2(node_neighbourhood, current_left_count, current_right_count);
      var x1 = self._distanceToCoord(node.distance);

      x1 = (x1 == 0) ? 2 : x1;

      var x2 = x1;
      var child_distance = function (child) {
        if (child.count == 1) {
          return self.distance;
        } else {
          return self._distanceToCoord(child.distance);
        }
      };
      var left_distance = child_distance(self.data.nodes[node.left_child]);
      var right_distance = child_distance(self.data.nodes[node.right_child]);
      if (right_child.count == 1) {
        y2 = y2 + self.pixels_for_leaf / 2;
      }

      self.dendrogram_layer.add(self._draw_horizontal_path(node_id, x1, y1, x2, y2, left_distance, right_distance));
      self._draw_row_dendrogram_node(node.left_child, left_child, current_left_count - node_neighbourhood.left_node.right_count, current_right_count + node_neighbourhood.left_node.right_count, left_distance, y1);
      self._draw_row_dendrogram_node(node.right_child, right_child, current_left_count + node_neighbourhood.right_node.left_count, current_right_count - node_neighbourhood.right_node.left_count, right_distance, y2);
    } else {
      var objects = node.objects;
      self.leaves_y_coordinates[node_id] = y;

      for (var i = 0, len = objects.length; i < len; i++) {
        self.objects2leaves[objects[i]] = node_id;
      }

      var count = node.objects.length;
      if (count < self.min_item_count) {
        self.min_item_count = count;
      }
      if (count > self.max_item_count) {
        self.max_item_count = count;
      }
    }

  };

  InCHlibEx.prototype._coordToDistance = function (x) {
    var self = this;
    if (self.settings.log_axis) {
      return (Math.max(0, Math.pow(10, (self.distance - (5 + x)) / self.distance_step + log10(self.minDistance)))).toExponential(2);
    } else {
      return ((self.distance - x) / self.distance_step).toExponential(2);
    }
  };

  InCHlibEx.prototype._distanceToCoord = function (d) {
    var self = this;
    if (self.minDistance == 0 && self.maxDistance == 0) {
      return self.distance;
    }

    if (self.settings.log_axis) {
      var dist = log10(d) - log10(self.minDistance);
      if (!Number.isFinite(dist)) {
    	dist = 0;
      }
      return self.distance - (5 + self.distance_step * dist);
    } else {
      return self.distance - self.distance_step * d;
    }
  };

  /** @override */
  InCHlibEx.prototype._draw_distance_scale = function (distance) {
    var self = this;

    if (!self.settings.navigation_toggle.distance_scale) {
      return;
    }
    var y1 = self.header_height + self.column_metadata_height + self.settings.column_metadata_row_height / 2 - 10;
    var y2 = y1;
    var x1 = 0;
    var x2 = self.distance;
    var path = new Kinetic.Line({
      points: [x1, y1, x2, y2],
      stroke: "black",
      listening: false
    })

    var circle = new Kinetic.Circle({
      x: x2,
      y: y2,
      radius: 3,
      fill: "black",
      listening: false
    })

    var number = 0;
    var marker_tail = 3;
    var marker_distance = x2;
    var marker_number_distance = self._hack_round(30 / self.distance_step * 10) / 10;
    var dist = self._coordToDistance(0);

    var marker_distance_step = self._hack_round(self.distance_step * marker_number_distance);
    var marker_counter = 0;

    var distance_number = new Kinetic.Text({
      x: 0,
      y: y1 - 20,
      text: dist,
      fontSize: 12,
      fontFamily: self.settings.font,
      fontStyle: 'bold',
      fill: 'black',
      align: 'right',
      listening: false,
    });
    self.dendrogram_layer.add(path, circle, distance_number);

    if (marker_distance_step == 0) {
      marker_distance_step = 0.5;
    }

    if (self.settings.log_axis) {
      var from = Math.ceil(log10(self.minDistance));
      var to = Math.ceil(Math.max(0, log10(self.maxDistance)));

      marker_counter = from;

      while (marker_counter != to) {
        marker_distance = self.distance - 5 + self.distance_step * (log10(self.minDistance) - marker_counter);
        path = new Kinetic.Line({
          points: [marker_distance, (y1 - marker_tail), marker_distance, (y2 + marker_tail)],
          stroke: "black",
          listening: false,
        });
        self.dendrogram_layer.add(path);

        marker_counter++;
      }
    } else {
      if (marker_number_distance > 0.1) {
        while (marker_distance > 0) {
          path = new Kinetic.Line({
            points: [marker_distance, (y1 - marker_tail), marker_distance, (y2 + marker_tail)],
            stroke: "black",
            listening: false,
          });
          self.dendrogram_layer.add(path);

          number = self._hack_round((number + marker_number_distance) * 10) / 10;
          if (number > 10) {
            number = self._hack_round(number);
          }

          marker_distance = marker_distance - marker_distance_step;
          marker_counter++;
        }
      }
    }

    var scale_mouseover_area = new Kinetic.Line({
      points: [x1, y1, x2, y2],
      stroke: "white",
      strokeWidth: 15,
      opacity: 0
    });
    self.scale_mouseover_layer = new Kinetic.Layer();
    self.scale_mouseover_layer.add(scale_mouseover_area);

    var y3 = y1 - 10;
    scale_mouseover_area.on("mouseenter", function (evt) {
      self._scale_mouseover_area_enter(evt.evt["layerX"], y3);
    });
    scale_mouseover_area.on("mousemove", function (evt) {
      self._scale_mouseover_area_move(evt.evt["layerX"], y3);
    });
    scale_mouseover_area.on("click", function (evt) {
      self._scale_mouseover_area_click(evt.evt["layerX"], y3);
    });
    scale_mouseover_area.on("mouseleave", function (evt) {
      self._scale_mouseover_area_leave();
    });
  };

  InCHlibEx.prototype._scale_mouseover_area_enter = function (x, y) {
    var self = this;

    var dist = Math.max(0.0, self._coordToDistance(x));

    self.cutoff_line_layer.destroyChildren();
    self.cutoff_distance_tooltip = self.objects_ref.tooltip_label.clone({x: x, y: y, id: 'cotoff_tooltip_label'});
    self.cutoff_distance_tooltip.add(self.objects_ref.tooltip_tag.clone({pointerDirection: 'down'}));
    self.cutoff_distance_tooltip.add(self.objects_ref.tooltip_text.clone({text: dist, id: 'cutoff_tooltip_tag'}));

    var minX = self.cutoff_distance_tooltip.width() / 2.0;
    self.cutoff_distance_tooltip.x((x < minX) ? minX : x);

    var y1 = self.header_height;
    var y2 = self.header_height + self.column_metadata_height + (self.heatmap_array.length + 0.5) * self.pixels_for_leaf;
    var cutoff_line = self.objects_ref.cutoff_line.clone({points: [0, y1, 0, y2], id: "cutoff_line"}).x(x);
    self.cutoff_line_layer.add(cutoff_line);
    self.cutoff_line_layer.add(self.cutoff_distance_tooltip);
    self.cutoff_line_layer.moveToTop();

    self.cutoff_line_layer.draw();
  };

  InCHlibEx.prototype._scale_mouseover_area_move = function (x, y) {
    var self = this;

    var dist = Math.max(0.0, self._coordToDistance(x));

    if (self.cutoff_distance_tooltip) {
      var minX = self.cutoff_distance_tooltip.width() / 2.0;
      var x1 = (x < minX) ? minX : x;
      self.cutoff_line_layer.find('#cotoff_tooltip_label')[0].x(x1);
      self.cutoff_line_layer.find('#cutoff_tooltip_tag')[0].text(dist);
      self.cutoff_line_layer.find('#cutoff_line')[0].x(x);
    }

    self.cutoff_line_layer.draw();
  };

  InCHlibEx.prototype._scale_mouseover_area_leave = function () {
    var self = this;

    self.cutoff_distance_tooltip = null;
    self.cutoff_line_layer.destroyChildren();

    self.cutoff_line_layer.draw();
  };

  InCHlibEx.prototype._scale_mouseover_area_click = function (x, y) {
    var self = this;

    self._reset_all_highlight();
    self._change_selection_state(self.selection_state.cutoff);

    var dist = self._coordToDistance(x);
    self._highlight_cutoff_cluster(dist);

    var y1 = self.header_height;
    var y2 = self.header_height + self.column_metadata_height + (self.heatmap_array.length + 0.5) * self.pixels_for_leaf;
    var cutoff_line = self.objects_ref.cutoff_line.clone({points: [0, y1, 0, y2], stroke: "#F00"}).x(x);

    self.cutoff_cluster_overlay.add(cutoff_line);
    self.cutoff_cluster_overlay.draw();
  };

  InCHlibEx.prototype._divide_cluster_by_distance = function (distance) {
    var self = this;

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

    var ret = [];
    for (key in custer_groups) {
      ret.push(custer_groups[key]);
    }

    return ret;
  }

  InCHlibEx.prototype._highlight_cutoff_cluster = function (distance) {
    var self = this;

    self.current_object_ids = self._divide_cluster_by_distance(distance);
    self.last_highlighted_cutoff_cluster = self.current_object_ids

    self._draw_cutoff_cluster_overlay(self.current_object_ids);
  };

  InCHlibEx.prototype._unhighlight_cutoff_cluster = function () {
    var self = this;
    if (self.last_highlighted_cutoff_cluster) {
      self.cutoff_cluster_overlay.destroyChildren();
      self.cutoff_cluster_overlay.draw();
      self.current_object_ids = [];
      self.last_highlighted_cutoff_cluster = null;
      self._change_selection_state(null);
    }
  };

  InCHlibEx.prototype._draw_cutoff_cluster_overlay = function (custer_groups) {
    var self = this;

    self.cutoff_cluster_group = new Kinetic.Group();

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
      self.cutoff_cluster_group.add(element);
    });
    cluster_borders.forEach(function (element, index, arrar) {
      self.cutoff_cluster_group.add(element);
    });

    self.cutoff_cluster_overlay.add(self.cutoff_cluster_group);
  };

})(jQuery);
