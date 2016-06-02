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

package t.common.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


@SuppressWarnings("serial")
public class Dataset extends ManagedItem {

  public Dataset() {}

  private String description;
  private String publicComment;
  private int numBatches;

  public Dataset(String title, String description, String comment, Date date, 
      String publicComment, int numBatches) {
    super(title, comment, date);
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
      if (isSharedDataset(d.getTitle())) {
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

  public static String userDatasetTitle(String userKey) {
    return "user-" + userKey;
  }

  public static String userSharedDatasetTitle(String userKey) {
    return "user-shared-" + userKey;
  }

  public static boolean isSharedDataset(String title) {
    return title.startsWith("user-shared-");
  }
  
  //TODO this also matchser user shared datasets.
  public static boolean isUserDataset(String title) {
    return title.startsWith("user-");
  }

  public static boolean isDataVisible(String datasetTitle, String userKey) {
    return datasetTitle.equals(userDatasetTitle(userKey)) || isSharedDataset(datasetTitle)
        || !datasetTitle.startsWith("user-");
  }
}
