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

package t.model;

import java.io.Serializable;
import java.util.*;

import t.model.sample.Attribute;
import t.model.sample.SampleLike;

/**
 * A sample class identifies a group of samples.
 */
@SuppressWarnings("serial")
public class SampleClass implements Serializable, SampleLike {

  public SampleClass() {}

  private Map<Attribute, String> data = new HashMap<Attribute, String>();

  public SampleClass(Map<Attribute, String> data) {
    //Copy to a new java.util.HashMap to ensure GWT serialisation is possible
    this.data = new HashMap<Attribute, String>(data);
  }

  public String apply(Attribute key) {
    return get(key);
  }

  @Override
  public String get(Attribute key) {
    return data.get(key);
  }
  
  public void put(Attribute key, String value) {
    data.put(key, value);
  }
  
  public Set<Attribute> getKeys() {
    return data.keySet();
  }

  public void remove(Attribute key) {
    data.remove(key);
  }

  public SampleClass copy() {
    return new SampleClass(getMap());
  }

  public SampleClass copyOnly(Collection<Attribute> attributes) {
    Map<Attribute, String> data = new HashMap<Attribute, String>();
    for (Attribute attribute : attributes) {
      data.put(attribute, get(attribute));
    }
    return new SampleClass(data);
  }

  public SampleClass copyWith(Attribute key, String value) {
    SampleClass sc = copy();
    sc.put(key, value);
    return sc;
  }

  public SampleClass copyWithout(Attribute key) {
    SampleClass sc = copy();
    if (sc.contains(key)) {
      sc.remove(key);
    }
    return sc;
  }

  public boolean contains(Attribute key) {
    return data.containsKey(key);
  }

  /**
   * Merge only keys that are not already present
   */
  public void mergeDeferred(SampleClass from) {
    for (Attribute k : from.getMap().keySet()) {
      if (!data.containsKey(k)) {
        data.put(k, from.get(k));
      }
    }
  }

  /**
   * Returns a copy of this sample class' constraint map.
   */
  public Map<Attribute, String> getMap() {
    return new HashMap<Attribute, String>(data);
  }

  /**
   * Is this SampleClass compatible with the other one? True iff shared keys have the same values.
   * Commutative.
   */
  public boolean compatible(SampleClass other) {
    for (Attribute k : data.keySet()) {
      if (other.contains(k) && !other.get(k).equals(get(k))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true iff the tested sample class contains all the keys of this one and they have the
   * same values. Non-commutative.
   */
  public boolean strictCompatible(SampleClass other) {
    for (Attribute k : data.keySet()) {
      if (!other.contains(k) || !other.get(k).equals(get(k))) {
        return false;
      }
    }
    return true;
  }

  public static Set<String> collect(List<? extends SampleClass> from, Attribute key) {
    Set<String> r = new HashSet<String>();
    for (SampleClass sc : from) {
      String x = sc.get(key);
      if (x != null) {
        r.add(x);
      }
    }
    return r;
  }

  /**
   * Produce a new SampleClass that contains only those keys that were shared between the two
   * classes and had the same values.
   */
  public SampleClass intersection(SampleClass other) {
    Set<Attribute> k1 = other.getMap().keySet();
    Set<Attribute> k2 = getMap().keySet();
    Set<Attribute> keys = new HashSet<Attribute>();
    keys.addAll(k1);
    keys.addAll(k2);
    Map<Attribute, String> r = new HashMap<Attribute, String>();
    for (Attribute k : keys) {
      if (k2.contains(k) && k1.contains(k) && get(k).equals(other.get(k))) {
        r.put(k, get(k));
      }
    }
    return new SampleClass(r);
  }

  public static SampleClass intersection(List<? extends SampleClass> from) {
    if (from.size() == 0) {
      // This is technically an error, but let's be forgiving.
      return new SampleClass();
    } else if (from.size() == 1) {
      return from.get(0);
    }
    SampleClass r = from.get(0);
    for (int i = 1; i < from.size(); i++) {
      r = r.intersection(from.get(i));
    }
    return r;
  }

  public static <T extends SampleClass> List<T> filter(T[] from, Attribute key, String constraint) {
    List<T> ff = Arrays.asList(from);
    return filter(ff, key, constraint);
  }

  public static <T extends SampleClass> List<T> filter(List<T> from, Attribute key,
      String constraint) {
    List<T> r = new ArrayList<T>();
    for (T sc : from) {
      if (sc.get(key).equals(constraint)) {
        r.add(sc);
      }
    }
    return r;
  }


  @Override
  public boolean equals(Object other) {
    if (other instanceof SampleClass) {
      return data.equals(((SampleClass) other).getMap());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return data.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SC(");
    for (Attribute k : data.keySet()) {
      sb.append(k.id() + ":" + data.get(k) + ",");
    }
    sb.append(")");
    return sb.toString();
  }
}
