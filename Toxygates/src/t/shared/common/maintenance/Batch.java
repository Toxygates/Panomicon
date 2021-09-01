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

package t.shared.common.maintenance;

import java.util.*;

import t.shared.common.ManagedItem;

@SuppressWarnings("serial")
public class Batch extends ManagedItem {

  private int numSamples;
  private Set<String> enabledInstances;
  private String dataset;

  public Batch() {}

  public Batch(String id, int numSamples, String comment, Date date,
      Set<String> enabledInstances, String dataset) {
    super(id, comment, date);
    this.numSamples = numSamples;
    this.enabledInstances = enabledInstances;
    this.dataset = dataset;
  }

  public Batch(String id, String comment) {
    this(id, 0, comment, new Date(), new HashSet<String>(), "");
  }

  public String getDataset() {
    return dataset;
  }

  public int getNumSamples() {
    return numSamples;
  }

  /**
   * Get the list of instance IDs for which this batch is visible.
   */
  public Set<String> getEnabledInstances() {
    return enabledInstances;
  }

  /**
   * Set the list of instance IDs for which this batch is visible.
   */
  public void setEnabledInstanceIds(Set<String> enabled) {
    this.enabledInstances = enabled;
  }

  public void setEnabledInstances(Set<Instance> enabled) {
    Set<String> ids = new HashSet<String>();
    for (Instance i : enabled) {
      ids.add(i.getId());
    }
    setEnabledInstanceIds(ids);
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

}
