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

package t.shared.viewer;

public enum FilterType {
  GT(false, false), LT(false, true), AbsGT(true, false), AbsLT(true, true);
  
  boolean abs;
  boolean upper;
  
  FilterType(boolean abs, boolean upper) {
    this.abs = abs;
    this.upper = upper;
  }
  
  public static FilterType parse(String x) {
    boolean abs = x.contains("|");
    boolean lower = x.contains(">");
    if (abs) {
      return lower? AbsGT : AbsLT;      
    }
    return lower ? GT : LT;
  }
  
  @Override
  public String toString() {
    StringBuilder r = new StringBuilder();
    if (abs) {
      r.append("|x|");
    } else {
      r.append("x");
    }
    
    if (upper) {
      r.append(" <= ");
    } else {
      r.append(" >= ");
    }
    return r.toString();
  }
  
}
