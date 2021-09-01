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

package t.shared.common;

import java.util.*;
import java.util.stream.Collectors;


@SuppressWarnings("serial")
public class Dataset extends ManagedItem {

  public Dataset() {}

  private String description;
  private String publicComment;
  private int numBatches;

  public Dataset(String id, String description, String comment, Date date, 
      String publicComment, int numBatches) {
    super(id, comment, date);
    this.description = description;
    this.publicComment = publicComment;
    this.numBatches = numBatches;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String getUserTitle() {
    return description;
  }

  public String getPublicComment() {
    return publicComment;
  }

  public Collection<Dataset> getSubDatasets() {
    List<Dataset> r = new ArrayList<Dataset>();
    r.add(this);
    return r;
  }
  
  public int getNumBatches() {
    return numBatches;
  }

  public static Collection<Dataset> groupUserShared(String userTitle, Collection<Dataset> from) {
    List<Dataset> r = new ArrayList<Dataset>();
    List<Dataset> shared = new ArrayList<Dataset>();
    for (Dataset d : from) {
      if (isSharedDataset(d.getId())) {
        shared.add(d);        
      } else {
        r.add(d);
      }
    }
    if (!shared.isEmpty()) {
      GroupedDataset gd = new GroupedDataset("grouped-autogen", userTitle, shared);
      r.add(gd);
    }
    return r;
  }

  public static Collection<Dataset> ungroup(Collection<Dataset> from) {
    List<Dataset> r = new ArrayList<Dataset>();
    for (Dataset d : from) {
      r.addAll(d.getSubDatasets());
    }
    return r;
  }

  public static String userDatasetId(String userKey) {
    return "user-" + userKey;
  }

  public static String userSharedDatasetId(String userKey) {
    return "user-shared-" + userKey;
  }
  
  public static boolean isInDefaultSelection(String title) {
    return !isSharedDataset(title);
  }

  public static List<Dataset> defaultSelection(List<Dataset> from) {
    return from.stream().filter(x -> isInDefaultSelection(x.getId())).collect(Collectors.toList());
  }
  
  public static boolean isSharedDataset(String id) {
    return id.startsWith("user-shared-");
  }
  
  // Note: this also matches user "shared" datasets.
  public static boolean isUserDataset(String id) {
    return id.startsWith("user-");
  }

  public static boolean isDataVisible(String datasetId, String userKey) {
    return datasetId.equals(userDatasetId(userKey)) || isSharedDataset(datasetId)
        || !datasetId.startsWith("user-");
  }
}
