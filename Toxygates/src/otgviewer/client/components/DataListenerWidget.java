/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import static t.viewer.client.StorageParser.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.*;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * A Composite that is also a DataViewListener. 
 * Has default implementations for the change listener methods.
 *
 */
public class DataListenerWidget extends Composite implements DataViewListener {

  private List<DataViewListener> listeners = new ArrayList<DataViewListener>();

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
  
  public ClientState state() {
    return new ClientState(chosenDatasets, chosenSampleClass,
        chosenProbes, chosenCompounds, chosenColumns,
        chosenCustomColumn, chosenItemLists, chosenGeneSet,
        chosenClusteringList);
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

  public void propagateTo(DataViewListener other) {
    other.datasetsChanged(chosenDatasets);
    other.sampleClassChanged(chosenSampleClass);
    other.probesChanged(chosenProbes);
    other.compoundsChanged(chosenCompounds);
    other.columnsChanged(chosenColumns);
    other.customColumnChanged(chosenCustomColumn);
    other.itemListsChanged(chosenItemLists);
    other.geneSetChanged(chosenGeneSet);
    other.clusteringListsChanged(chosenClusteringList);
  }

  public StorageParser getParser(Screen s) {
    return s.manager().getParser();    
  }

  /**
   * Store this widget's state into local storage.
   */
  public void storeState(Screen s) {
    StorageParser p = getParser(s);
    storeState(p);
  }

  /**
   * Store this widget's state into local storage.
   */
  public void storeState(StorageParser p) {
    storeColumns(p);
    storeProbes(p);
    storeGeneSet(p);
  }

  protected void storeColumns(StorageParser p, String key,
      Collection<? extends SampleColumn> columns) {
    if (!columns.isEmpty()) {
      SampleColumn first = columns.iterator().next();
      String representative =
          (first.getSamples().length > 0) ? first.getSamples()[0].toString()
              : "(no samples)";

      logger.info("Storing columns for " + key + " : " + first + " : "
          + representative + " ...");
      p.setItem(key, packColumns(columns));
    } else {
      logger.info("Clearing stored columns for: " + key);
      p.clearItem(key);
    }
  }

  public void storeColumns(StorageParser p) {
    storeColumns(p, "columns", chosenColumns);
  }

  protected void storeCustomColumn(StorageParser p, DataColumn<?> column) {
    if (column != null) {
      p.setItem("customColumn", column.pack());
    } else {
      p.clearItem("customColumn");
    }
  }

  // Separator hierarchy for columns:
  // ### > ::: > ^^^ > $$$
  protected List<Group> loadColumns(StorageParser p, DataSchema schema,
      String key, Collection<? extends SampleColumn> expectedColumns, AttributeSet attributes)
      throws Exception {
    // TODO unpack old format columns
    String v = p.getItem(key);
    List<Group> r = new ArrayList<Group>();
    if (v != null && !v.equals(packColumns(expectedColumns))) {
      String[] spl = v.split("###");
      for (String cl : spl) {
        Group c = unpackColumn(schema, cl, attributes);
        r.add(c);
      }
      return r;
    }
    return null;
  }

  public void storeProbes(StorageParser p) {
    p.setItem("probes", packProbes(chosenProbes));
  }

  public void storeItemLists(StorageParser p) {
    p.setItem("lists", packItemLists(chosenItemLists, "###"));
  }

  public void storeGeneSet(StorageParser p) {
    p.setItem("geneset", (chosenGeneSet != null ? chosenGeneSet.pack() : ""));
  }

  public void storeClusteringLists(StorageParser p) {
    p.setItem("clusterings", packItemLists(chosenClusteringList, "###"));
  }
  
  private String packCompounds(StorageParser p) {
    return StorageParser.packList(chosenCompounds, "###");
  }
  
  public void storeCompounds(StorageParser p) {
    p.setItem("compounds", packCompounds(p));
  }
  
  private String packDatasets(StorageParser p) {
    List<String> r = new ArrayList<String>();
    for (Dataset d: chosenDatasets) {
      r.add(d.getTitle());
    }
    return StorageParser.packList(r, "###");
  }
  
  public void storeDatasets(StorageParser p) {  
    p.setItem("datasets", packDatasets(p));
  }
  
  public void storeSampleClass(StorageParser p) {
    if (chosenSampleClass != null) {
      p.setItem("sampleClass", t.common.client.Utils.packSampleClass(chosenSampleClass));
    }    
  }
  
  public List<ItemList> loadItemLists(StorageParser p) {
    return loadLists(p, "lists");
  }

  public List<ItemList> loadClusteringLists(StorageParser p) {
    return loadLists(p, "clusterings");
  }

  public List<ItemList> loadLists(StorageParser p, String name) {
    List<ItemList> r = new ArrayList<ItemList>();
    String v = p.getItem(name);
    if (v != null) {
      String[] spl = v.split("###");
      for (String x : spl) {
        ItemList il = ItemList.unpack(x);
        if (il != null) {
          r.add(il);
        }
      }
    }
    return r;
  }
  
  public void loadDatasets(StorageParser p) {
    String v = p.getItem("datasets");
    if (v == null || v.equals(packDatasets(p))) {
      return;
    }
    List<Dataset> r = new ArrayList<Dataset>();
    for (String ds: v.split("###")) {
      r.add(new Dataset(ds, "", "", null, ds, 0));
    }
    changeDatasets(r.toArray(new Dataset[0]));
  }
  
  public void loadCompounds(StorageParser p) {
    String v = p.getItem("compounds");
    if (v == null || v.equals(packCompounds(p))) {
      return;
    }
    List<String> r = new ArrayList<String>();    
    for (String c: v.split("###")) {
      r.add(c);
    }
    changeCompounds(r);
  }
  
  public void loadSampleClass(StorageParser p, AttributeSet attributes) {
    String v = p.getItem("sampleClass");
    if (v == null || v.equals(t.common.client.Utils.packSampleClass(chosenSampleClass))) {
      return;
    }
    SampleClass sc = t.common.client.Utils.unpackSampleClass(attributes, v);
    changeSampleClass(sc);    
  }

  /**
   * Load saved state from the local storage. 
   * If the loaded state is different from what was previously remembered in this widget, the appropriate 
   * signals will fire.
   */
  public void loadState(Screen sc, AttributeSet attributes) {
    StorageParser p = getParser(sc);
    loadState(p, sc.schema(), attributes);
  }

  public void loadState(StorageParser p, DataSchema schema, AttributeSet attributes) {
    SampleClass sc = new SampleClass();
    // Note: currently the "real" sample class, as chosen by the user on the
    // column screen for example, is not stored, and hence not propagated
    // between screens.
    sampleClassChanged(sc);

    try {
      List<Group> cs = loadColumns(p, schema, "columns", chosenColumns, attributes);
      if (cs != null) {
        logger.info("Unpacked columns: " + cs.get(0) + ": "
            + cs.get(0).getSamples()[0] + " ... ");
        columnsChanged(cs);
      }
      Group g = unpackColumn(schema, p.getItem("customColumn"), attributes);
      if (g != null) {
        customColumnChanged(g);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to load state", e);
      // one possible failure source is if data is stored in an incorrect
      // format
      columnsChanged(new ArrayList<Group>());
      storeColumns(p); // overwrite the old data
      storeCustomColumn(p, null); // ditto
      logger.log(Level.WARNING, "Exception while parsing state", e);
    }

    String v = p.getItem("probes");
    if (v != null && !v.equals("") && !v.equals(packProbes(chosenProbes))) {
      chosenProbes = v.split("###");
      probesChanged(chosenProbes);
    } else if (v == null || v.equals("")) {
      probesChanged(new String[0]);
    }
    List<ItemList> lists = loadItemLists(p);
    if (lists.size() > 0) {
      chosenItemLists = lists;
      itemListsChanged(lists);
    }
    
    ItemList geneSet = ItemList.unpack(p.getItem("geneset"));
    if (geneSet != null) {
      chosenGeneSet = geneSet;
    }
    geneSetChanged(geneSet);
    
    lists = loadClusteringLists(p);
    if (lists.size() > 0) {
      chosenClusteringList = lists;
      clusteringListsChanged(lists);
    }
    
    //Note: the ordering of the following 3 is important
    loadDatasets(p);
    loadSampleClass(p, attributes);
    loadCompounds(p);
  }

  private int numPendingRequests = 0;

  private DialogBox waitDialog;

  // Load indicator handling
  protected void addPendingRequest() {
    numPendingRequests += 1;
    if (numPendingRequests == 1) {
      if (waitDialog == null) {
        waitDialog = Utils.waitDialog();
      } else {
        waitDialog.setPopupPositionAndShow(Utils.displayInCenter(waitDialog));
      }
    }
  }

  protected void removePendingRequest() {
    numPendingRequests -= 1;
    if (numPendingRequests == 0) {
      waitDialog.hide();
    }
  }

  protected List<Sample> getAllSamples() {
    List<Sample> list = new ArrayList<Sample>();
    for (Group g : chosenColumns) {
      List<Sample> ss = Arrays.asList(g.getSamples());
      list.addAll(ss);
    }
    return list;
  }
}
