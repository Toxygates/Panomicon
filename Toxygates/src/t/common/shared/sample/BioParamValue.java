/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A measured value of a biological parameter, such as the total kidney weight.
 */
@SuppressWarnings("serial")
abstract public class BioParamValue implements Serializable, Comparable<BioParamValue> {

  protected String id, label, section;
  
  public BioParamValue() {}
  
  /**
   * @param id ID string of the parameter
   * @param label Human-readable label of the parameter
   */
  public BioParamValue(String id, String label, @Nullable String section) {
    this.id = id;
    this.label = label;
    this.section = section;
  }
  
  public String label() { return label; }
  public String id() { return id; }
  
  /**
   * Human-readable observed value.
   * @return
   */
  abstract public String displayValue();
  
  public String tooltip() { return displayValue(); } 

  public boolean isPathological() { return false; }
  
  /**
   * Undefined values are specifically recorded as "undefined" in the database,
   * since a measurement attempt was made but the result was recorded as undefined.
   * This is distinct from a NaN or missing value.
   */
  public boolean isDefined() { return true; }
  
  public @Nullable String section() { return section; }
  
  public int compareTo(BioParamValue other) {
    return label().compareTo(other.label());
  }
}
