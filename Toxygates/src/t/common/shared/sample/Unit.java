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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import t.common.shared.DataSchema;
import t.model.SampleClass;

/**
 * A sample class with associated samples.
 * 
 * TODO: generify, split up OTG/non-OTG versions
 */
@SuppressWarnings("serial")
public class Unit extends SampleClass {

  private Sample[] samples;

  public Unit() {}

  public Unit(SampleClass sc, Sample[] samples) {
    super(sc.getMap());
    this.samples = samples;
  }

  public Sample[] getSamples() {
    return samples;
  }

  public void averageAttributes(NumericalBioParamValue[] params) {
    Sample first = samples[0];
    for (NumericalBioParamValue p : params) {
      if (first.sampleClass().get(p.id) != null) {
        double sum = 0.0;
        for (Sample sample : samples) {
          sum += Double.parseDouble(sample.sampleClass().get(p.id));
        }
        put(p.id, Double.toString(sum));
      }
    }
    // TODO: maybe some exception checking for when conversion to double fails, or when
    // some of the later samples are missing a parameter that the first sample had
  }

  public void concatenateAttributes(StringBioParamValue[] params) {
    String separator = "/";

    Sample firstSample = samples[0];
    for (StringBioParamValue p : params) {
      if (firstSample.sampleClass().get(p.id) != null) {
        HashSet<String> values = new HashSet<String>();
        String concatenation = "";

        Boolean firstIteration = true;
        for (Sample sample : samples) {
          String newValue = sample.sampleClass().get(p.id);
          if (!values.contains(newValue)) {
            values.add(newValue);
            if (!firstIteration) {
              concatenation += separator;
            } else {
              firstIteration = false;
            }
            concatenation += newValue;
          }
        }
        put(p.id, concatenation);
      }
    }
    // TODO: maybe more robust handling of when only some of the samples have a value for
    // a given parameter. Or maybe we can assume that won't happen.
  }

  public static Unit[] formUnits(DataSchema schema, Sample[] samples) { 
    return formUnits(schema, Arrays.asList(samples)).toArray(new Unit[] {});
  }

  public static List<Unit> formUnits(DataSchema schema, Collection<Sample> samples) {
    if (samples.size() == 0) {
      return new ArrayList<Unit>();
    }
    Map<SampleClass, List<Sample>> groups = new HashMap<SampleClass, List<Sample>>();
    for (Sample os : samples) {
      SampleClass unit = SampleClassUtils.asUnit(os.sampleClass(), schema);
      if (!groups.containsKey(unit)) {
        groups.put(unit, new ArrayList<Sample>());
      }
      groups.get(unit).add(os);
    }

    List<Unit> r = new ArrayList<Unit>(groups.keySet().size());
    for (SampleClass u : groups.keySet()) {
      Unit uu = new Unit(u, groups.get(u).toArray(new Sample[] {}));
      r.add(uu);
    }
    return r;
  }

  public static Sample[] collectBarcodes(Unit[] units) {
    List<Sample> r = new ArrayList<Sample>();
    for (Unit b : units) {
      Collections.addAll(r, b.getSamples());
    }
    return r.toArray(new Sample[0]);
  }

  public static boolean contains(Unit[] units, String param, String value) {
    return SampleClass.filter(units, param, value).size() > 0;
  }
}
