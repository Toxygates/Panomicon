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

package t.model;

import java.io.Serializable;
import java.util.*;

import t.model.sample.Attribute;
import t.model.sample.SampleLike;


/**
 * A sample class identifies a group of samples.
 * 
 * Standard keys for OTG: time, dose, organism, organ_id, test_type, sin_rep_type 
 * Optional keys:
 * compound_name, exposure_time, dose_level 
 */
@SuppressWarnings("serial")
public class SampleClass implements Serializable, SampleLike {

  public SampleClass() {}

  private Map<String, String> data = new HashMap<String, String>();

  public SampleClass(Map<String, String> data) {
    //Copy to a new java.util.HashMap to ensure GWT serialisation is possible
    this.data = new HashMap<String, String>(data);
  }

  public String apply(Attribute key) {
    return get(key);
  }
  
  @Deprecated
  public String apply(String key) {
    return get(key);
  }
  
  @Override
  public String get(Attribute key) {
    return data.get(key.id());
  }
  
  @Deprecated
  public String get(String key) {
    return data.get(key);
  }

  @Deprecated
  public void put(String key, String value) {
    data.put(key, value);
  }
  
  public void put(Attribute key, String value) {
    data.put(key.id(), value);
  }
  
  // TODO: this should return a set of attributes, but we can't do that until
  // we switch to using attributes as hashmap keys.
  @Deprecated
  public Set<String> getKeys() {
    return data.keySet();
  }

  public void remove(String key) {
    data.remove(key);
  }

  public SampleClass copy() {
    return new SampleClass(getMap());
  }

  public SampleClass copyOnly(Collection<Attribute> attributes) {
    Map<String, String> data = new HashMap<String, String>();
    for (Attribute attribute : attributes) {
      data.put(attribute.id(), get(attribute));
    }
    return new SampleClass(data);
  }

  public SampleClass copyWith(String key, String value) {
    SampleClass sc = copy();
    sc.put(key, value);
    return sc;
  }
  
  public SampleClass copyWithout(String key) {
    SampleClass sc = copy();
    if (sc.contains(key)) {
      sc.remove(key);
    }
    return sc;
  }

  public boolean contains(String key) {
    return data.containsKey(key);
  }

  public boolean contains(Attribute key) {
    return data.containsKey(key.id());
  }

  /**
   * Merge only keys that are not already present
   * @param from
   */
  public void mergeDeferred(SampleClass from) {
    for (String k : from.getMap().keySet()) {
      if (!data.containsKey(k)) {
        data.put(k, from.get(k));
      }
    }
  }

  /**
   * Returns a copy of this sample class' constraint map.
   * @return
   */
  public Map<String, String> getMap() {
    return new HashMap<String, String>(data);
  }

  /**
   * Is this SampleClass compatible with the other one? True iff shared keys have the same values.
   * Commutative.
   * 
   * @param other
   * @return
   */
  public boolean compatible(SampleClass other) {
    for (String k : data.keySet()) {
      if (other.contains(k) && !other.get(k).equals(get(k))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true iff the tested sample class contains all the keys of this one and they have the
   * same values. Non-commutative.
   * 
   * @param other
   * @return
   */
  public boolean strictCompatible(SampleClass other) {
    for (String k : data.keySet()) {
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
   * 
   * @param other
   * @return
   */
  public SampleClass intersection(SampleClass other) {
    Set<String> k1 = other.getMap().keySet();
    Set<String> k2 = getMap().keySet();
    Set<String> keys = new HashSet<String>();
    keys.addAll(k1);
    keys.addAll(k2);
    Map<String, String> r = new HashMap<String, String>();
    for (String k : keys) {
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

  public static <T extends SampleClass> List<T> filter(T[] from, String key, String constraint) {
    List<T> ff = Arrays.asList(from);
    return filter(ff, key, constraint);
  }

  public static <T extends SampleClass> List<T> filter(List<T> from, String key, String constraint) {
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
    for (String k : data.keySet()) {
      sb.append(k + ":" + data.get(k) + ",");
    }
    sb.append(")");
    return sb.toString();
  }

  //TODO move out of OTGTool
  public String pack() {
    StringBuilder sb = new StringBuilder();
    for (String k : data.keySet()) {
      sb.append(k + ",,,");
      sb.append(data.get(k) + ",,,");
    }
    return sb.toString();
  }

  //TODO move out of OTGTool
  public static SampleClass unpack(String data) {
    String[] spl = data.split(",,,");
    Map<String, String> d = new HashMap<String, String>();
    for (int i = 0; i < spl.length; i += 2) {
      d.put(spl[i], spl[i + 1]);
    }
    return new SampleClass(d);
  }
}
