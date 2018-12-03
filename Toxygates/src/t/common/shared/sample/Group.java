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

package t.common.shared.sample;

import java.util.*;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.StorageParser;

/**
 * A group of barcodes.
 */
@SuppressWarnings("serial")
public class Group extends SampleGroup<Sample> implements SampleColumn {

  protected Unit[] _units;

  public Group() {}

  public Group(DataSchema schema, String name, Sample[] barcodes, String color) {
    super(schema, name, barcodes, color);
    _units = Unit.formUnits(schema, barcodes);
  }

  public Group(DataSchema schema, String name, Sample[] barcodes) {
    super(schema, name, barcodes);
    _units = Unit.formUnits(schema, barcodes);
  }

  public Group(DataSchema schema, String name, Unit[] units) {
    super(schema, name, Unit.collectBarcodes(units));
    _units = units;
  }

  @Override
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

  public String tooltipText(DataSchema schema) {
    SampleClass sc = getSamples()[0].sampleClass();
    return SampleClassUtils.label(sc, schema) + ":\n" + getTriples(schema, -1, ", ");
  }

  @Override
  public String pack() {
    StringBuilder s = new StringBuilder();
    s.append("Group:::");
    s.append(name + ":::"); // !!
    s.append(color + ":::");
    for (Sample sample : _samples) {
      s.append(StorageParser.packSample(sample));
      s.append("^^^");
    }
    return s.toString();
  }

  public static Group unpack(DataSchema schema, String s, AttributeSet attributeSet) {
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
      Sample b = StorageParser.unpackSample(s2[i], attributeSet);
      bcs[i] = b;
    }
    // DataFilter useFilter = (bcs[0].getUnit().getOrgan() == null) ? filter : null;
    return new Group(schema, name, bcs, color);

  }

  public static List<Sample> getAllSamples(List<Group> columns) {
    List<Sample> list = new ArrayList<Sample>();
    for (Group g : columns) {
      List<Sample> ss = Arrays.asList(g.getSamples());
      list.addAll(ss);
    }
    return list;
  }

  public boolean hasSameUnits(Group otherGroup) {
    if (_units.length == otherGroup._units.length) {
      Set<Unit> units = new HashSet<Unit>(Arrays.asList(_units));
      for (Unit unit : otherGroup._units) {
        if (!units.contains(unit)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
