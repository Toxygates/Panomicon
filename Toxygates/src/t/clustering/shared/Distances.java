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
package t.clustering.shared;

public enum Distances {
  EUCLIDIAN("euclidean"), MAXIMUM("maximum"), MANHATTAN("manhattan"), CANBERRA("canberra"), BINARY(
      "binary"), PEARSON("pearson"), ABSPEARSON("abspearson"), COERRELATION(
          "correlation"), ABSORRELATION("abscorrelation"), SPEARMAN("spearman"), KENDALL("kendall");
  private String distance;

  private Distances(String distance) {
    this.distance = distance;
  }

  public String asParam() {
    return distance;
  }

  /** 
   *  Return an enum constant that matches given distance.
   *  Note that the usage of this function differs from Enum#valueOf(String).
   *  
   *  @see Enum#valueOf(Class, String)
   */
  static public Distances lookup(String distance) {
    for (Distances d : Distances.values()) {
      if (d.distance.equalsIgnoreCase(distance)) {
        return d;
      }
    }
    return null;
  }

}
