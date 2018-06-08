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

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.Composite;

import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;

/**
 * A Composite that is also a DataViewListener. 
 * Has default implementations for the change listener methods.
 *
 */
public class DataListenerWidget extends Composite implements DataViewListener {

  protected List<DataViewListener> listeners = new ArrayList<DataViewListener>();

  protected Dataset[] chosenDatasets = new Dataset[0];
  protected SampleClass chosenSampleClass; 
  protected String[] chosenProbes = new String[0];
  protected List<String> chosenCompounds = new ArrayList<String>();
  protected List<Group> chosenColumns = new ArrayList<Group>();
  protected SampleColumn chosenCustomColumn;
  //TODO this should not be public
  public List<ItemList> chosenItemLists = new ArrayList<ItemList>(); 
  protected ItemList chosenGeneSet = null;
  protected List<ItemList> chosenClusteringList = new ArrayList<ItemList>();

  protected final Logger logger = SharedUtils.getLogger("dlwidget");

  public Logger getLogger() {
    return logger;
  }

  public List<Group> chosenColumns() {
    return this.chosenColumns;
  }

  public DataListenerWidget() {
    super();
  }

  public void addListener(DataViewListener l) {
    listeners.add(l);
  }

  // incoming signals
  @Override
  public void datasetsChanged(Dataset[] ds) {
    chosenDatasets = ds;
    changeDatasets(ds);
  }

  @Override
  public void sampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;
    changeSampleClass(sc);
  }

  @Override
  public void probesChanged(String[] probes) {
    chosenProbes = probes;
    changeProbes(probes);
  }

  @Override
  public void availableCompoundsChanged(List<String> compounds) {
    changeAvailableCompounds(compounds);
  }

  @Override
  public void compoundsChanged(List<String> compounds) {
    chosenCompounds = compounds;
    changeCompounds(compounds);
  }
  
  @Override
  public void columnsChanged(List<Group> columns) {
    chosenColumns = columns;
    changeColumns(columns);
  }

  @Override
  public void customColumnChanged(SampleColumn customColumn) {
    this.chosenCustomColumn = customColumn;
    changeCustomColumn(customColumn);
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    this.chosenItemLists = lists;
    changeItemLists(lists);
  }

  @Override
  public void geneSetChanged(ItemList geneSet) {
    this.chosenGeneSet = geneSet;
    changeGeneSet(geneSet);
  }

  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    this.chosenClusteringList = lists;
    changeClusteringLists(lists);
  }

  // outgoing signals

  protected void changeDatasets(Dataset[] ds) {
    chosenDatasets = ds;
    for (DataViewListener l : listeners) {
      l.datasetsChanged(ds);
    }
  }

  protected void changeSampleClass(SampleClass sc) {
    chosenSampleClass = sc;
    for (DataViewListener l : listeners) {
      l.sampleClassChanged(sc);
    }
  }

  protected void changeProbes(String[] probes) {
    chosenProbes = probes;
    for (DataViewListener l : listeners) {
      l.probesChanged(probes);
    }
  }

  /**
   * Change the available compounds
   * 
   * @param compounds
   */
  protected void changeAvailableCompounds(List<String> compounds) {
    for (DataViewListener l : listeners) {
      l.availableCompoundsChanged(compounds);
    }
  }

  /**
   * Change the selected compounds
   * 
   * @param compounds
   */
  protected void changeCompounds(List<String> compounds) {
    chosenCompounds = compounds;
    assert (compounds != null);
    for (DataViewListener l : listeners) {
      l.compoundsChanged(compounds);
    }
  }

  protected void changeColumns(List<Group> columns) {
    chosenColumns = columns;
    assert (columns != null);
    for (DataViewListener l : listeners) {
      l.columnsChanged(columns);
    }
  }

  protected void changeCustomColumn(SampleColumn customColumn) {
    this.chosenCustomColumn = customColumn;
    for (DataViewListener l : listeners) {
      l.customColumnChanged(customColumn);
    }
  }

  protected void changeItemLists(List<ItemList> lists) {
    chosenItemLists = lists;
    for (DataViewListener l : listeners) {
      l.itemListsChanged(lists);
    }
  }

  protected void changeGeneSet(ItemList geneSet) {
    chosenGeneSet = geneSet;
    for (DataViewListener l : listeners) {
      l.geneSetChanged(geneSet);
    }
  }

  protected void changeClusteringLists(List<ItemList> lists) {
    chosenClusteringList = lists;
    for (DataViewListener l : listeners) {
      l.clusteringListsChanged(lists);
    }
  }

  public List<Sample> getAllSamples() {
    List<Sample> list = new ArrayList<Sample>();
    for (Group g : chosenColumns) {
      List<Sample> ss = Arrays.asList(g.getSamples());
      list.addAll(ss);
    }
    return list;
  }
}
