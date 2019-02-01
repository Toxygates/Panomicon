/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otg.viewer.client.charts;

import java.util.Map;

import javax.annotation.Nullable;

import t.common.shared.DataSchema;
import t.common.shared.sample.Sample;

/**
 * A ColorPolicy is a way of coloring data points in a chart.
 */
class ColorPolicy {

  final String defaultColor = "grey";

  @Nullable
  String colorFor(DataPoint sample) {
    return defaultColor;
  }

  static class TimeDoseColorPolicy extends ColorPolicy {
    String timeDose;
    String color;

    TimeDoseColorPolicy(String timeDose, String color) {
      this.timeDose = timeDose;
      this.color = color;
    }

    @Override
    String colorFor(DataPoint point) {
      DataSchema schema = point.schema();
      if (schema.getMinor(point).equals(timeDose) || schema.getMedium(point).equals(timeDose)) {
        return color;
      }
      return super.colorFor(point);
    }
  }

  static class MapColorPolicy extends ColorPolicy {
    Map<? extends Sample, String> colors;

    MapColorPolicy(Map<? extends Sample, String> colors) {
      this.colors = colors;
    }

    @Override
    String colorFor(DataPoint point) {
      if (colors.containsKey(point.sample)) {
        return colors.get(point.sample);
      }
      return super.colorFor(point);
    }
  }
}
