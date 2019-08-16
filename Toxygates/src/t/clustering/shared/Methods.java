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
package t.clustering.shared;

public enum Methods {
  WARD_D("ward.D"), WARD_D2("ward.D2"), SINGLE("single"), COMPLETE("complete"), AVERAGE(
      "average"), MCQUITTY("mcquitty"), MEDIAN("median"), CENTROID("centroid");

  private String method;

  Methods(String method) {
    this.method = method;
  }

  public String asParam() {
    return method;
  }

  /** 
   *  Return an enum constant that matches given method.
   *  Note that the usage of this function differs from Enum#valueOf(String).
   *  
   *  @see Enum#valueOf(Class, String)
   */
  static public Methods lookup(String method) {
    for (Methods m : Methods.values()) {
      if (m.method.equalsIgnoreCase(method)) {
        return m;
      }
    }
    return null;
  }

}
