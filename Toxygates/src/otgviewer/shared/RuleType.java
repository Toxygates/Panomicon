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

package otgviewer.shared;

import java.io.Serializable;

/**
 * Rule types for compound ranking. See otgviewer.server.Conversions for conversions to the Scala
 * equivalents.
 * 
 * @author johan
 *
 */
public enum RuleType implements Serializable {
  Sum("Total upregulation"), NegativeSum("Total downregulation"), Synthetic("User pattern"), MaximalFold(
      "Maximal fold"), MinimalFold("Minimal fold"), ReferenceCompound("Reference compound"), MonotonicUp(
      "Monotonic up"), MonotonicDown("Monotonic down"), Unchanged("Unchanged"), LowVariance(
      "Low variance"), HighVariance("High variance");

  private String name;

  private RuleType(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }

  public static RuleType parse(String s) {
    for (RuleType rt : RuleType.values()) {
      if (s.equals(rt.toString())) {
        return rt;
      }
    }
    return null;
  }
}
