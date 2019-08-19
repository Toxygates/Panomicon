/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.common.shared.sample;

import java.util.*;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.model.SampleClass;

/**
 * A group of samples.
 */
@SuppressWarnings("serial")
public class Group extends SampleGroup<Sample> implements SampleColumn {

  protected Unit[] _units;

  public Group() {}

  public Group(DataSchema schema, String name, Sample[] sampleIds, String color,
      Unit[] units) {
    super(schema, name, sampleIds, color);
    _units = units;
  }
  
  public Group(DataSchema schema, String name, Sample[] sampleIds, String color) {
    super(schema, name, sampleIds, color);
    _units = Unit.formUnits(schema, sampleIds);
  }

  public Group(DataSchema schema, String name, Sample[] sampleIds) {
    super(schema, name, sampleIds);
    _units = Unit.formUnits(schema, sampleIds);
  }

  public Group(DataSchema schema, String name, Unit[] units) {
    super(schema, name, Unit.collectSamples(units));
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
