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

import java.io.Serializable;

import t.common.shared.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.SampleLike;

@SuppressWarnings("serial")
public class Sample implements Packable, Serializable, HasClass, SampleLike {

  public Sample() {}

  public Sample(String _code, SampleClass _sampleClass) {
    id = _code;
    sampleClass = _sampleClass;
  }

  protected SampleClass sampleClass;

  private String id;

  public String id() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Sample) {
      Sample that = (Sample) other;
      return id.equals(that.id());
    }
    return false;
  }

  @Override
  public SampleClass sampleClass() {
    return sampleClass;
  }

  @Override
  public String get(Attribute attribute) {
    return sampleClass.get(attribute);
  }
  
  //TODO deprecate when the time is right
  public String get(String parameter) {
    return sampleClass.get(parameter);
  }

  public boolean contains(Attribute key) {
    return sampleClass.contains(key);
  }

  public String getTitle(DataSchema schema) {
    return getShortTitle(schema) + " (" + id() + ")";
  }

  public String getShortTitle(DataSchema schema) {
    return get(schema.mediumParameter()) + "/" + get(schema.minorParameter());
  }

  @Override
  public String toString() {
    return sampleClass.toString();
  }

  public static Sample unpack(String s) {
    String[] spl = s.split("\\$\\$\\$");
    String v = spl[0];
    if (!v.equals("Barcode_v3")) {
      // Incorrect/legacy format - TODO: warn here
      return null;
    }
    String id = spl[1];
    SampleClass sc = SampleClass.unpack(spl[2]);
    return new Sample(id, sc);
  }

  @Override
  public String pack() {
    final String sep = "$$$";
    StringBuilder sb = new StringBuilder();
    sb.append("Barcode_v3").append(sep);
    sb.append(id()).append(sep);
    sb.append(sampleClass.pack()).append(sep);
    return sb.toString();
  }
}
