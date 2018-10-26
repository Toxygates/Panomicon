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

package t.clustering.shared;

import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import t.common.shared.SharedUtils;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

@SuppressWarnings("serial")
public class ClusteringList extends ItemList {

  public static final String USER_CLUSTERING_TYPE = "userclustering";
  
  @Nullable
  private Algorithm algorithm;
  private Map<String, String> params; // optional

  private StringList[] clusters;

  /**
   * This constructor should NOT be used except unpacking from stored item
   * To instantiate ClusteringList, use <code>ClusteringList(String, String, Algorithm, StringList[])</code>
   * @see t.viewer.shared.ItemList#unpack(String)
   */
  public ClusteringList(String type, String name, String[] items) {
    super(type, name);

    this.algorithm = extractAlgorithm(items[0]);
    this.params = extractParams(items[0]);

    List<StringList> clusters = new ArrayList<StringList>();
    for (String s : Arrays.copyOfRange(items, 1, items.length)) {
      String[] spl = s.split("\\$\\$\\$");
      clusters.add(new StringList(StringList.PROBES_LIST_TYPE, 
          spl[0], Arrays.copyOfRange(spl, 1, spl.length)));
    }

    this.clusters = clusters.toArray(new StringList[0]);
  }

  public ClusteringList(String type, String name, 
      @Nullable Algorithm algorithm, StringList[] clusters) {
    super(type, name);

    this.algorithm = algorithm;
    // use TreeMap to ensure order of key
    this.params = new TreeMap<String, String>();
    this.clusters = clusters;
  }
  
  public StringList[] asStringLists() { return clusters; }
  
  @Nullable
  public Algorithm algorithm() { return algorithm; }
  // return cloned map
  public Map<String, String> params() { return new TreeMap<String, String>(params); }

  public void addParam(String name, String value) {
    if (name.contains("=")) {
      throw new IllegalArgumentException("Given parameter name contains unaccptable character.");
    }
    if (params.containsKey(name)) {
      throw new IllegalArgumentException("Given parameter name is already taken.");
    }
    params.put(name, value);
  }
  
  public static final String UNKNOWN_TOKEN = "Unknown";
  
  // pack informations for algorithm and other parameters.
  // (e.g.) If algorithm = { Row(ward.D, correlation), Col(ward.D2, euclidean) }
  // and params = { "cutoff" -> "1.0", "some" -> "value" },
  // it returns "ward.D,correlation,ward.D2,euclidean$$$cutoff=1.0$$$some=value$$$"
  private String packedHeader() {
    List<String> items = new ArrayList<String>();

    if (algorithm == null) {
      items.add(UNKNOWN_TOKEN);
    } else {
      items.add(algorithm.toString());
    }
    for (Entry<String, String> e : params.entrySet()) {
      items.add(e.getKey() + "=" + e.getValue());
    }

    return SharedUtils.packList(items, "$$$");
  }

  @Nullable
  private Algorithm extractAlgorithm(String header) {
    // the algorithm should be the first element of splitted items
    String[] spl = header.split("\\$\\$\\$");
    if (spl.length < 1) {
      // return default algorithm
      return new Algorithm();
    }
    if (spl[0].equals(UNKNOWN_TOKEN)) {
      return null;
    }

    // splitting with comma is dependent on the implementation of
    // t.common.shared.userclustering.Algorithm#toString()
    String[] algo = spl[0].split(",");
    if (algo.length != 4) {
      return new Algorithm();
    }

    Methods rowMethod = Methods.lookup(algo[0]);
    Distances rowDistance = Distances.lookup(algo[1]);
    Methods colMethod = Methods.lookup(algo[2]);
    Distances colDistance = Distances.lookup(algo[3]);
    if (rowMethod == null || rowDistance == null || colMethod == null || colDistance == null) {
      return new Algorithm();
    }

    return new Algorithm(rowMethod, rowDistance, colMethod, colDistance);
  }

  private Map<String, String> extractParams(String header) {
    Map<String, String> params = new TreeMap<String, String>();

    String[] spl = header.split("\\$\\$\\$");
    // the parameters should be from second(index=1) element
    for (String s : Arrays.copyOfRange(spl, 1, spl.length)) {
      String[] param = s.split("=");
      if (param.length != 2) {
        continue;
      }
      params.put(param[0], param[1]);
    }

    return params;
  }

  protected String pack(StringList list) {
    StringBuilder sb = new StringBuilder();
    sb.append(list.name());
    // because available separator is only "$" currently,
    // combine name and list with "$".
    // We may consider defining more separators.
    sb.append("$$$");
    sb.append(SharedUtils.packList(list.packedItems(), "$$$"));
    return sb.toString();
  }

  @Override
  public Collection<String> packedItems() {
    List<String> items = new ArrayList<String>();
    items.add(packedHeader());
    for (StringList l : clusters) {
      items.add(pack(l));
    }

    return items;
  }

  public StringList[] items() {
    return clusters;
  } 

  @Override
  public int size() {
    if (clusters == null) {
      return 0;
    } else {
      return clusters.length;
    }
  }

  public static List<ClusteringList> pickUserClusteringLists(Collection<? extends ItemList> from,
      @Nullable String title) {
    List<ClusteringList> r = new LinkedList<ClusteringList>();
    for (ItemList l : from) {
      if (l.type().equals(USER_CLUSTERING_TYPE) 
          && (title == null || l.name().equals(title))) {
        r.add((ClusteringList) l);
      }
    }
    return r;
  }

}
