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

package t.viewer.shared.clustering;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import t.viewer.shared.StringList;

@SuppressWarnings("serial")
public class ProbeClustering implements Serializable {

  public final static String PROBE_CLUSTERING_TYPE = "probeclustering";
  
  private Clusterings clustering;
  private StringList list;

  public StringList getList() {
    return list;
  }
  
  public Clusterings getClustering() {
    return clustering;
  }

  // need for gwt serialization
  protected ProbeClustering() {}

  public ProbeClustering(Clusterings clustering, StringList probes) {
    this.clustering = clustering;
    this.list = probes;
  }

  /**
   * Build a ProbeClustering from a StringList.
   * 
   * @param list
   * @return null if failed to parse title
   */
  public static ProbeClustering buildFrom(StringList list) {
    // currently, support only hierarchical clustering
    Clusterings cl = HierarchicalClustering.parse(list.name());
    if (cl == null) {
      return null;
    }
    
    List<String> path = new ArrayList<>();
    path.add(cl.algorithm.getTitle());
    path.add(cl.clustering);
    for (Entry<String, String> e : cl.params.entrySet()) {
      path.add(e.getValue());
    }
    path.add(cl.cluster);

    return new ProbeClustering(cl, new StringList(PROBE_CLUSTERING_TYPE, list.name(), 
      list.items()));
  }

  public static Collection<ProbeClustering> filterByAlgorithm(Collection<ProbeClustering> from,
      final Algorithm algorithm) {
    return new ProbeClusteringFilter() {
      @Override
      protected boolean accept(ProbeClustering pc) {
        return pc.clustering.getAlgorithm() == algorithm;
      }
    }.filter(from);
  }

  public static Collection<ProbeClustering> filterByClustering(Collection<ProbeClustering> from,
      final String clustering) {
    return new ProbeClusteringFilter() {
      @Override
      protected boolean accept(ProbeClustering pc) {
        return pc.clustering.getClustering().equals(clustering);
      }
    }.filter(from);
  }

  public static Collection<ProbeClustering> filterByParam(Collection<ProbeClustering> from,
      final String paramName, final String paramValue) {
    return new ProbeClusteringFilter() {
      @Override
      protected boolean accept(ProbeClustering pc) {
        if (!pc.clustering.getParams().containsKey(paramName)) {
          return false;
        }
        if (!pc.clustering.getParams().get(paramName).equals(paramValue)) {
          return false;
        }
        return true;
      }
    }.filter(from);
  }
  public static List<String> collectParamValue(Collection<ProbeClustering> from, String paramName) {
    Set<String> result = new HashSet<String>();
    for (ProbeClustering pc : from) {
      result.add(pc.clustering.getParams().get(paramName));
    }
    return new ArrayList<String>(result);
  }

}

abstract class ProbeClusteringFilter {
  protected abstract boolean accept(ProbeClustering pc);

  public Collection<ProbeClustering> filter(Collection<ProbeClustering> from) {
    Collection<ProbeClustering> result = new ArrayList<ProbeClustering>();
    for (ProbeClustering pc : from) {
      if (accept(pc)) {
        result.add(pc);
      }
    }

    return result;
  }
}
