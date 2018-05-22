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

package otgviewer.client.components;

import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.ItemList;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.viewer.client.StorageParser;
import t.viewer.shared.intermine.IntermineInstance;

public interface ImportingScreen extends Screen {
  boolean importProbes(String[] probes);

  boolean importColumns(List<Group> groups);

  void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists);

  void runEnrichment(@Nullable IntermineInstance preferredInstance);

  List<ItemList> clusteringList();

  List<ItemList> itemLists();

  void clusteringListsChanged(List<ItemList> lists);

  void itemListsChanged(List<ItemList> lists);

  void storeClusteringLists(StorageParser p);

  void storeItemLists(StorageParser p);

  List<Group> chosenColumns();

  List<Sample> getAllSamples();

  void propagateTo(DataViewListener other);

  void storeState();
}
