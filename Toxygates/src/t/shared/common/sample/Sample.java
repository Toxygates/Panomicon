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

import java.io.Serializable;
import java.util.Set;

import t.shared.common.DataSchema;
import t.shared.common.HasClass;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.SampleLike;

@SuppressWarnings("serial")
public class Sample implements Serializable, HasClass, SampleLike {

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
  
  public Set<Attribute> getKeys() {
    return sampleClass.getKeys();
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
}
