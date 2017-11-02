/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import java.util.*;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.model.sample.AttributeSet;

/**
 * A group of barcodes.
 */
@SuppressWarnings("serial")
public class Group extends SampleGroup<Sample> implements SampleColumn {

  // TODO lift units up
  protected Unit[] _units;

  public Group() {}

  public Group(DataSchema schema, String name, Sample[] barcodes, String color) {
    super(schema, name, barcodes, color);
    // TODO unit formation will not work if the barcodes have different sample classes
    // - fix
    if (barcodes.length > 0) {
      _units = Unit.formUnits(schema, barcodes);
    } else {
      _units = new Unit[] {};
    }
  }

  public Group(DataSchema schema, String name, Sample[] barcodes) {
    super(schema, name, barcodes);
    // TODO unit formation will not work if the barcodes have different sample classes
    // - fix
    if (barcodes.length > 0) {
      _units = Unit.formUnits(schema, barcodes);
    } else {
      _units = new Unit[] {};
    }
  }

  public Group(DataSchema schema, String name, Unit[] units) {
    super(schema, name, Unit.collectBarcodes(units));
    _units = units;
  }

  public Group(DataSchema schema, String name, Unit[] units, String color) {
    this(schema, name, Unit.collectBarcodes(units), color);
  }

  public String getShortTitle() {
    return name;
  }

  @Override
  public Sample[] getSamples() {
    return _samples;
  }

  public Sample[] getTreatedSamples() {
    return Arrays.stream(_units).
      filter(u -> !schema.isSelectionControl(u)).
      flatMap(u -> Arrays.stream(u.getSamples())).toArray(Sample[]::new);    
  }

  public Sample[] getControlSamples() {
    return Arrays.stream(_units).
        filter(u -> schema.isSelectionControl(u)).
        flatMap(u -> Arrays.stream(u.getSamples())).toArray(Sample[]::new);    
  }

  public Unit[] getUnits() {
    return _units;
  }

  public String getTriples(DataSchema schema, int limit, String separator) {
    Set<String> triples = new HashSet<String>();
    boolean stopped = false;
    for (Unit u : _units) {
      if (schema.isControlValue(u.get(schema.mediumParameter()))) {
        continue;
      }
      if (triples.size() < limit || limit == -1) {
        triples.add(SampleClassUtils.tripleString(u, schema));
      } else {
        stopped = true;
        break;
      }
    }
    String r = SharedUtils.mkString(triples, separator);
    if (stopped) {
      return r + "...";
    } else {
      return r;
    }
  }

  // See SampleGroup for the packing method
  // TODO lift up the unpacking code to have
  // the mirror images in the same class, if possible
  public static Group unpack(DataSchema schema, String s, AttributeSet attributeSet) {
    // Window.alert(s + " as group");
    String[] s1 = s.split(":::"); // !!
    String name = s1[1];
    String color = "";
    String barcodes = "";

    color = s1[2];
    barcodes = s1[3];
    if (SharedUtils.indexOf(groupColors, color) == -1) {
      // replace the color if it is invalid.
      // this lets us safely upgrade colors in the future.
      color = groupColors[0];
    }


    String[] s2 = barcodes.split("\\^\\^\\^");
    Sample[] bcs = new Sample[s2.length];
    for (int i = 0; i < s2.length; ++i) {
      Sample b = Sample.unpack(s2[i], attributeSet);
      bcs[i] = b;
    }
    // DataFilter useFilter = (bcs[0].getUnit().getOrgan() == null) ? filter : null;
    return new Group(schema, name, bcs, color);

  }

}
