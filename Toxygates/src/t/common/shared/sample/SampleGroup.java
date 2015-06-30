/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.shared.sample;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import otgviewer.shared.Group;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;

/**
 * A way of grouping microarray samples. Unique colors for each group can be generated.
 * 
 * @author johan
 *
 * @param <S>
 */
@SuppressWarnings("serial")
public class SampleGroup<S extends Sample> implements DataColumn<S>, Serializable,
    Comparable<SampleGroup<?>> {

  /**
   * This list was generated using the service at http://tools.medialab.sciences-po.fr/iwanthue/
   */
  protected static final String[] groupColors = new String[] {"#97BDBD", "#C46839", "#9F6AC8",
      "#9CD05B", "#513C4D", "#6B7644", "#C75880"};
  private static int nextColor = 0;


  public SampleGroup() {}

  protected String name;
  protected String color;
  protected S[] _samples;
  protected DataSchema schema;

  public SampleGroup(DataSchema schema, String name, S[] samples, String color) {
    this.name = name;
    this._samples = samples;
    this.color = color;
    this.schema = schema;
  }

  public SampleGroup(DataSchema schema, String name, S[] samples) {
    this(schema, name, samples, pickColor());
  }

  private static synchronized String pickColor() {
    nextColor += 1;
    if (nextColor > groupColors.length) {
      nextColor = 1;
    }
    return groupColors[nextColor - 1];
  }

  public S[] samples() {
    return _samples;
  }

  public S[] getSamples() {
    return _samples;
  }

  public String getName() {
    return name;
  }

  public String getShortTitle(DataSchema schema) {
    return name;
  }

  public String toString() {
    return name;
  }

  public String getColor() {
    return color;
  }

  public String getStyleName() {
    return "Group" + getColorIndex();
  }

  private int getColorIndex() {
    return SharedUtils.indexOf(groupColors, color);
  }

  public String pack() {
    StringBuilder s = new StringBuilder();
    s.append("Group:::");
    s.append(name + ":::"); // !!
    s.append(color + ":::");
    for (S b : _samples) {
      s.append(b.pack());
      s.append("^^^");
    }
    return s.toString();
  }


  @Override
  public int compareTo(SampleGroup<?> other) {
    return name.compareTo(other.getName());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(_samples) * 41 + name.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SampleGroup) {
      SampleGroup<?> that = (SampleGroup<?>) other;
      return that.canEqual(this) && Arrays.deepEquals(this._samples, that.samples())
          && name.equals(that.getName()) && color.equals(that.getColor());
    }
    return false;
  }

  protected boolean canEqual(Object other) {
    return other instanceof SampleGroup;
  }

  public Set<String> collect(String parameter) {
    return SampleClass.collectInner(Arrays.asList(_samples), parameter);
  }

  public static Set<String> collectAll(Iterable<Group> from, String parameter) {
    Set<String> r = new HashSet<String>();
    for (Group g : from) {
      r.addAll(g.collect(parameter));
    }
    return r;
  }

}
