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

package t.viewer.client;

import java.util.List;

import t.common.shared.Dataset;
import t.common.shared.ItemList;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.model.SampleClass;

/**
 * Captures the current client-side state.
 */
public class ClientState {
  
  public ClientState(Dataset[] datasets, SampleClass sampleClass,
      String[] probes, List<String> compounds,
      List<Group> columns,
      SampleColumn customColumn, List<ItemList> itemLists,
      ItemList geneSet,
      List<ItemList> chosenClusteringList) {
    
    this.sampleClass = sampleClass;
    this.datasets = datasets;
    this.probes = probes;
    this.compounds = compounds;
    this.columns = columns;
    this.customColumn = customColumn;
    this.itemLists = itemLists;
    this.chosenClusteringList = chosenClusteringList;
    this.geneSet = geneSet;
  }
  
  public final Dataset[] datasets;
  public final SampleClass sampleClass; 
  public final String[] probes;
  public final List<String> compounds;
  public final List<Group> columns;
  public final SampleColumn customColumn;
  public final List<ItemList> itemLists, chosenClusteringList;
  public final ItemList geneSet;  
}
