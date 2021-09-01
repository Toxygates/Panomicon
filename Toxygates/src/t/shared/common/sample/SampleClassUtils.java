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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import t.shared.common.DataSchema;
import t.shared.common.HasClass;
import t.model.SampleClass;
import t.model.sample.Attribute;

public class SampleClassUtils {
  public static SampleClass asMacroClass(SampleClass sc, DataSchema schema) {
    return sc.copyOnly(Arrays.asList(schema.macroParameters()));
  }

  public static SampleClass asUnit(SampleClass sc, DataSchema schema) {
    List<Attribute> keys = new ArrayList<Attribute>();
    for (Attribute attribute : schema.macroParameters()) {
      keys.add(attribute);
    }
    keys.add(schema.majorParameter());
    keys.add(schema.mediumParameter());
    keys.add(schema.minorParameter());
    return sc.copyOnly(keys);
  }
  
  public static String label(SampleClass sc, DataSchema schema) {
    StringBuilder sb = new StringBuilder();
    for (Attribute attribute : schema.macroParameters()) {
      sb.append(sc.get(attribute)).append("/");
    }
    return sb.toString();
  }
  
  public static String tripleString(SampleClass sc, DataSchema schema) {
    String maj = sc.get(schema.majorParameter());
    String med = sc.get(schema.mediumParameter());
    String min = sc.get(schema.minorParameter());
    return maj + "/" + med + "/" + min;
  }

  public static Stream<String> collectInner(List<? extends HasClass> from, Attribute key) {
    return from.stream().map(hc -> hc.sampleClass().get(key)).
      filter(k -> k != null).distinct();
  }

  public static boolean strictCompatible(SampleClass sc, HasClass hc2) {
    return sc.strictCompatible(hc2.sampleClass());
  }

  public static List<SampleClass> classes(List<? extends HasClass> from) {
    List<SampleClass> r = new ArrayList<SampleClass>();
    for (HasClass hc : from) {
      r.add(hc.sampleClass());
    }
    return r;
  }

  public static <S extends Sample, HS extends HasSamples<S>> 
      Stream<String> getMajors(DataSchema schema, HS hasSamples) {
    return getMajors(schema, hasSamples, null);
  }

  public static <S extends Sample, HS extends HasSamples<S>> 
      Stream<String> getMajors(DataSchema schema, HS hasSamples, @Nullable SampleClass sc) {
    List<S> sList = Arrays.asList(hasSamples.getSamples());
    List<S> filtered = (sc != null) ? filter(sc, sList) : sList;
    return collectInner(filtered, schema.majorParameter())
        .filter(v -> !schema.isMajorParamSharedControl(v));
  }
  
  public static <T extends HasClass> List<T> filter(SampleClass sc, List<T> from) {
    return from.stream().filter(test -> sc.compatible(test.sampleClass())).
         collect(Collectors.toList());   
  }
}
