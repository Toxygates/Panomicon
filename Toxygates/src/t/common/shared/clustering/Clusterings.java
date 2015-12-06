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

package t.common.shared.clustering;

import java.io.Serializable;
import java.util.Map;

/**
 * Class Clustering is basic concept for clustering informations
 *
 */
@SuppressWarnings("serial")
public abstract class Clusterings implements Serializable {
  protected Algorithm algorithm;
  protected String clustering; // e.g. "LV"
  protected Map<String, String> params; // e.g. { K -> 120 }
  protected String cluster; // e.g. "C1"
  protected String title; // "LV_K120_C1"

  public Clusterings() {}

  public Clusterings(Algorithm algorithm, String clustering, Map<String, String> params,
      String cluster, String title) {
    this.algorithm = algorithm;
    this.clustering = clustering;
    this.params = params;
    this.cluster = cluster;
    this.title = title;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public String getClustering() {
    return clustering;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public String getCluster() {
    return cluster;
  }

  public String getTitle() {
    return title;
  }
}
