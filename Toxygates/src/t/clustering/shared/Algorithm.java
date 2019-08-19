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
package t.clustering.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Algorithm implements Serializable {

  private Methods rowMethod;
  private Distances rowDistance;
  private Methods colMethod;
  private Distances colDistance;

  public Algorithm() {
    this(Methods.WARD_D, Distances.COERRELATION, Methods.WARD_D, Distances.COERRELATION);
  }

  public Algorithm(Methods rowMethod, Distances rowDistance, Methods colMethod,
      Distances colDistance) {
    this.rowMethod = rowMethod;
    this.rowDistance = rowDistance;
    this.colMethod = colMethod;
    this.colDistance = colDistance;
  }

  public Methods getRowMethod() {
    return rowMethod;
  }

  public Distances getRowDistance() {
    return rowDistance;
  }

  public Methods getColMethod() {
    return colMethod;
  }

  public Distances getColDistance() {
    return colDistance;
  }

  /**
   * Note that generated string is based on the value returned by asParam()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(rowMethod.asParam());
    sb.append(",");
    sb.append(rowDistance.asParam());
    sb.append(",");
    sb.append(colMethod.asParam());
    sb.append(",");
    sb.append(colDistance.asParam());

    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Algorithm)) {
      return false;
    }
    Algorithm algo = (Algorithm) obj;

    return rowMethod.equals(algo.rowMethod) && rowDistance.equals(algo.rowDistance)
        && colMethod.equals(algo.colMethod) && colDistance.equals(algo.colDistance);
  }
}
