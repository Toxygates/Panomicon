/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.viewer.shared;

import java.io.Serializable;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class ColumnFilter implements Serializable {

  public static ColumnFilter emptyAbsGT = new ColumnFilter(null, FilterType.AbsGT);
  public static ColumnFilter emptyLT =  new ColumnFilter(null, FilterType.LT);
  
  /**
   * GWT constructor
   */
  public ColumnFilter() {}
  
  public FilterType filterType;
  
  /**
   * Threshold value for this filter.
   */
  public @Nullable Double threshold;
  
  public ColumnFilter(@Nullable Double threshold, FilterType filterType) {
    this.threshold = threshold;
    this.filterType = filterType;
  }
  
  /**
   * Does a given value pass this filter?
   * @return
   */
  public boolean test(Double x) {
    if (threshold == null) {
      return true;
    }
    
    boolean upper = filterType.upper;
    double v = filterType.abs ? Math.abs(x) : x;
    return (upper && v <= threshold) ||        
        (!upper && v >= threshold);
  }
  
  public boolean active() {
    return threshold != null;
  }
  
  public String toString() {
    return filterType.toString() + " " + threshold;
  }
  
  public ColumnFilter asInactive() {
    return new ColumnFilter(null, filterType);
  }
}
