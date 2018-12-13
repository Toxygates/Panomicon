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

import java.util.Arrays;

import javax.annotation.Nullable;

import t.common.shared.DataSchema;
import t.common.shared.HasClass;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.viewer.client.Utils;

public class ChartSample implements HasClass {
  final SampleClass sc;
  final DataSchema schema;

  final double value;
  final char call;
  final @Nullable Sample sample;
  final String probe;
  String color = "grey";
  final String label;

  ChartSample(SampleClass sc, DataSchema schema, double value, Sample sample, String probe,
      char call, @Nullable String label) {

    this.sc = sc.copyOnly(Arrays.asList(schema.chartParameters()));
    this.schema = schema;
    this.value = value;
    this.sample = sample;
    this.probe = probe;
    this.call = call;
    this.label = label;
  }

  ChartSample(Sample sample, DataSchema schema, double value, String probe, char call,
      @Nullable String label) {
    this(sample.sampleClass(), schema, value, sample, probe, call, label);
  }

  ChartSample(Unit u, DataSchema schema, double value, String probe, char call) {
    this(u, schema, value, u.getSamples()[0], // representative sample only
        probe, call, "");
  }

  public DataSchema schema() {
    return schema;
  }

  public SampleClass sampleClass() {
    return sc;
  }

  public double value() {
    return value;
  }

  public Sample sample() {
    return sample;
  }

  public char call() {
    return call;
  }

  public String color() {
    return color;
  }

  public String probe() {
    return probe;
  }

  public String formattedValue() {
    String r = (label != null ? label : "");
    return r + "\n" + Utils.formatNumber(value()) + ":" + call();
  }
}
