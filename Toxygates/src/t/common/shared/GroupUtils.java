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

package t.common.shared;

import java.util.*;
import java.util.stream.Stream;

import t.common.shared.sample.Sample;
import t.common.shared.sample.SampleGroup;
import t.model.sample.Attribute;

/**
 * Data manipulation utility methods.
 * 
 * @author johan
 *
 */
public class GroupUtils {

  /**
   * In the list of groups, find the one that has the given title.
   * 
   * @param groups
   * @param title
   * @return
   */
  public static <T extends Sample, G extends SampleGroup<T>> 
  Optional<G> findGroup(List<G> groups,
      String title) {
    return groups.stream().filter(g -> g.getName().equals(title)).findFirst();    
  }

  /**
   * In the list of groups, find those that contain the given sample.
   * 
   * @param columns
   * @param barcode
   * @return
   */
  public static <T extends Sample, G extends SampleGroup<T>> 
    Stream<G> groupsFor(List<G> columns,
      String barcode) {
    return columns.stream().filter(c -> c.containsSample(barcode));  
  }

  public static Set<String> collect(List<? extends SampleGroup<?>> columns, Attribute parameter) {
    Set<String> r = new HashSet<String>();
    for (SampleGroup<?> g : columns) {
      for (String c : g.collect(parameter)) {
        r.add(c);
      }
    }
    return r;
  }

  /**
   * Extract the sample that has the given id from the list of groups.
   * 
   * @param columns
   * @param sample
   * @return
   */
  public static <T extends Sample> T sampleFor(List<? extends SampleGroup<T>> columns, String id) {
    for (SampleGroup<T> c : columns) {
      for (T t : c.getSamples()) {
        if (t.id().equals(id)) {
          return t;
        }
      }
    }
    return null;
  }


}
