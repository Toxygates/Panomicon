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

package t.shared.common.sample;

import java.util.*;
import java.util.stream.Stream;

import t.shared.common.DataSchema;
import t.shared.common.SharedUtils;
import t.model.SampleClass;

/**
 * A group of samples.
 */
@SuppressWarnings("serial")
public class Group extends SampleGroup<Sample> implements SampleColumn {

  protected Unit[] treatedUnits;
  protected Unit[] controlUnits;

  //GWT constructor
  public Group() {}

  public Group(DataSchema schema, String name, Sample[] sampleIds, String color) {
    super(name, sampleIds, color);

    Unit[] all = Unit.formUnits(schema, sampleIds);
    controlUnits = Arrays.stream(all).filter(u -> schema.isControl(u)).toArray(Unit[]::new);
    treatedUnits = Arrays.stream(all).filter(u -> !schema.isControl(u)).toArray(Unit[]::new);
  }

  public Group(String name, Unit[] treatedUnits, Unit[] controlUnits, String color) {
    super(name, Unit.collectSamples(treatedUnits, controlUnits), color);
    this.treatedUnits = treatedUnits;
    this.controlUnits = controlUnits;
  }

  public Group(DataSchema schema, String name, Sample[] sampleIds) {
    this(schema, name, Unit.formUnits(schema, sampleIds));
  }

  public Group(DataSchema schema, String name, Unit[] units) {
    super(name, Unit.collectSamples(units));

    controlUnits = Arrays.stream(units).filter(u -> schema.isControl(u)).toArray(Unit[]::new);
    treatedUnits = Arrays.stream(units).filter(u -> !schema.isControl(u)).toArray(Unit[]::new);
  }

  public Group(String name, Unit[] treatedUnits, Unit[] controlUnits) {
    super(name, Unit.collectSamples(treatedUnits, controlUnits));
    this.treatedUnits = treatedUnits;
    this.controlUnits = controlUnits;
  }

  @Override
  public String getShortTitle() {
    return name;
  }

  @Override
  public Sample[] getSamples() {
    return samples;
  }

  public Sample[] getTreatedSamples() {
    return Arrays.stream(treatedUnits).
      flatMap(u -> Arrays.stream(u.getSamples())).toArray(Sample[]::new);    
  }

  public Sample[] getControlSamples() {
    return Arrays.stream(controlUnits).
      flatMap(u -> Arrays.stream(u.getSamples())).toArray(Sample[]::new);
  }

  public Unit[] getUnits() {
    return Stream.concat(Arrays.stream(treatedUnits),
      Arrays.stream(controlUnits)).
      toArray(Unit[]::new);
  }

  public Unit[] getTreatedUnits() { return treatedUnits; }
  public Unit[] getControlUnits() { return controlUnits; }

  public String getTriples(DataSchema schema, int limit, String separator) {
    Set<String> triples = new HashSet<String>();
    boolean stopped = false;
    for (Unit u : treatedUnits) {
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
    Unit[] thisUnits = getUnits();
    Unit[] otherUnits = otherGroup.getUnits();
    if (thisUnits.length == otherUnits.length) {
      Set<Unit> units = new HashSet<Unit>(Arrays.asList(thisUnits));
      for (Unit unit : otherUnits) {
        if (!units.contains(unit)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
