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

package t.common.shared;

import java.util.*;

import t.model.SampleClass;
import t.model.sample.Attribute;

/**
 * A SampleMultiFilter is a filter for SampleClass and HasClass. For each key, several permitted
 * values may be specified.
 * 
 * @author johan
 */
public class SampleMultiFilter {

  private Map<Attribute, Set<String>> constraints = new HashMap<Attribute, Set<String>>();

  public SampleMultiFilter() {}

  public SampleMultiFilter(Map<Attribute, Set<String>> constr) {
    constraints = constr;
  }

  public boolean contains(Attribute key) {
    return constraints.containsKey(key);
  }

  public void addPermitted(Attribute key, String value) {
    if (constraints.containsKey(key)) {
      constraints.get(key).add(value);
    } else {
      Set<String> v = new HashSet<String>();
      v.add(value);
      constraints.put(key, v);
    }
  }

  public void addPermitted(Attribute key, String[] value) {
    for (String v : value) {
      addPermitted(key, v);
    }
  }

  /**
   * Returns true if and only if the SampleClass contains one of the permitted values for all keys
   * specified in this multi filter.
   * 
   * @param sc
   * @return
   */
  public boolean accepts(SampleClass sc) {
    for (Attribute k : constraints.keySet()) {
      Set<String> vs = constraints.get(k);
      if (!sc.contains(k) || !vs.contains(sc.get(k))) {
        return false;
      }
    }
    return true;
  }

  public boolean accepts(HasClass hc) {
    return accepts(hc.sampleClass());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Attribute k : constraints.keySet()) {
      sb.append(k + ":(");
      for (String v : constraints.get(k)) {
        sb.append(v + ",");
      }
      sb.append(")");
    }
    return "SMF(" + sb.toString() + ")";
  }
}
