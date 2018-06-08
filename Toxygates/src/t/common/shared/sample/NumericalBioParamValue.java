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

import static t.common.client.Utils.formatNumber;

import javax.annotation.Nullable;

/**
 * A numerical biological parameter with a pathological range and a healthy range.
 */
@SuppressWarnings("serial")
public class NumericalBioParamValue extends BioParamValue {

  protected @Nullable Double lowerBound, upperBound;
  protected double value;

  public NumericalBioParamValue() {}

  /**
   * @param id
   * @param label
   * @param lowerBound Lower bound on the healthy range, if any
   * @param upperBound Upper bound on the healthy range, if any
   * @param value Observed value
   */
  public NumericalBioParamValue(String id, String label, @Nullable String section,
      @Nullable Double lowerBound,
      @Nullable Double upperBound, double value) {
    super(id, label, section);
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.value = value;
  }

  public NumericalBioParamValue(String id, String label,
      @Nullable String section,
      @Nullable Double lowerBound, @Nullable Double upperBound, 
      String value) {
    super(id, label, section);
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    if (value != null) {
      try {
        this.value = Double.parseDouble(value);
      } catch (NumberFormatException e) {
        this.value = Double.NaN;
      }      
    } else {
      this.value = Double.NaN;
    }
  }

  public double value() {
    return value;
  }
  
  public boolean isAbove() {
    if (upperBound == null) {
      return false;
    } 
    return value > upperBound;    
  }
  
  public boolean isBelow() {
    if (lowerBound == null) {
      return false;
    }
    return value < lowerBound;
  }

  @Override
  public boolean isPathological() {
    return isAbove() || isBelow();
  }

  @Override
  public String displayValue() {
    return formatNumber(value);
  }

  @Override
  public String tooltip() {
    if (lowerBound != null && upperBound != null) {
      return "Normal range: " + formatNumber(lowerBound) + " <= x <= " + formatNumber(upperBound);
    }
    if (lowerBound != null) {
      return "Normal range: " + formatNumber(lowerBound) + " <= x";
    }
    if (upperBound != null) {
      return "Normal range: x <= " + formatNumber(upperBound);
    }
    return displayValue();
  }
}
