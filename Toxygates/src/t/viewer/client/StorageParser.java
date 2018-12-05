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

package t.viewer.client;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;

import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Packer.UnpackInputException;
import t.viewer.client.network.PackedNetwork;
import t.viewer.shared.AppInfo;
import t.viewer.shared.ItemList;

/**
 * Refactoring in progress. Methods for converting objects to and from strings
 * have been moved to Packer.
 * 
 * Storage parsing/serialising code. PersistedState may also be considered for
 * some of these items in the future, where lifecycle management is needed.
 */
public class StorageParser {

  private final String prefix;
  private final Storage storage;
  private static final char[] reservedChars = new char[] {':', '#', '$', '^'};
  public static final String unacceptableStringMessage =
      "The characters ':', '#', '$' and '^' are reserved and may not be used.";

  protected static final Logger logger = SharedUtils.getLogger("storage");

  public final SampleClassPacker sampleClassPacker;
  public final SamplePacker samplePacker;
  public final GroupPacker groupPacker;
  public final ColumnsPacker columnsPacker;
  
  public final ListPacker<String> probesPacker = 
      new ListPacker<String>(new IdentityPacker(), "###");
  public final ListPacker<String> compoundsPacker = 
      new ListPacker<String>(new IdentityPacker(), "###");
  public final ListPacker<Dataset> datasetsPacker = 
      new ListPacker<Dataset>(new DatasetPacker(), "###");
  public final ListPacker<ItemList> itemListsPacker = 
      new ListPacker<ItemList>(new ItemListPacker(), "###");
  public final ListPacker<ItemList> clusteringListsPacker = 
      new ListPacker<ItemList>(new ItemListPacker(), "###");
  public final ItemListPacker genesetPacker = new ItemListPacker();
  public final ListPacker<PackedNetwork> packedNetworksPacker =
      new ListPacker<PackedNetwork>(new PackedNetworkPacker(), "###");

  public StorageParser(Storage storage, String prefix, AttributeSet attributes, DataSchema schema) {
    this.prefix = prefix;
    this.storage = storage;

    sampleClassPacker = new SampleClassPacker(attributes);
    samplePacker = new SamplePacker(sampleClassPacker);
    groupPacker = new GroupPacker(samplePacker, schema);
    columnsPacker = new ColumnsPacker(groupPacker);
  }

  public String packSample(Sample sample) {
    return samplePacker.pack(sample);
  }

  public Sample unpackSample(String string) throws UnpackInputException {
    return samplePacker.unpack(string);
  }

  public void setItem(String key, String value) {
    storage.setItem(prefix + "." + key, value);
    // logger.info("SET " + prefix + "." + key + " -> " + value);
  }

  public String getItem(String key) {
    String v = storage.getItem(prefix + "." + key);
    // logger.info("GET " + prefix + "." + key + " -> " + v);
    return v;
  }

  public void clearItem(String key) {
    storage.removeItem(prefix + "." + key);
  }

  public static boolean isAcceptableString(String test, String failMessage) {
    for (char c : reservedChars) {
      if (test.indexOf(c) != -1) {
        Window.alert(failMessage + " " + unacceptableStringMessage);
        return false;
      }
    }
    return true;
  }

  // TBD: encapsulate all these "get" and "store" operations, with all their 
  // error/contingency handling, into objects, like we did with all the Packers.

  @Nullable
  public SampleClass getSampleClass() {
    String v = getItem("sampleClass");
    if (v == null) {
      return new SampleClass();
    } else {
      return sampleClassPacker.unpack(v);
    }
  }

  @Nullable
  public List<Group> getColumns(String key) throws Exception {
    return columnsPacker.unpack(getItem(key));
  }

  public List<Group> getChosenColumns() {
    try {
      return getColumns("columns");
    } catch (Exception e) {
      logger.log(Level.WARNING, "Exception while retrieving columns", e);
      return new ArrayList<Group>();
    }
  }

  public Group getCustomColumn() throws UnpackInputException {
    return groupPacker.unpack(getItem("customColumn"));
  }

  public String[] getProbes() {
    String probeString = getItem("probes");
    if (probeString != null) {
      try {
        return probesPacker.unpack(probeString).toArray(new String[0]);
      } catch (UnpackInputException e) {
        return null; // this will never happen
      }
    } else {
      return new String[0];
    }
  }

  public Dataset[] getDatasets(AppInfo info) {
    String v = getItem("datasets");
    if (v == null || v == "") {      
      return Dataset.defaultSelection(info.datasets());        
    }
    try {
      return datasetsPacker.unpack(v).toArray(new Dataset[0]);
    } catch (UnpackInputException e) {
      return null; // this will never happen
    }
    
  }

  public List<String> getCompounds() {
    String v = getItem("compounds");
    if (v == null) {
      return new ArrayList<String>();
    }
    try {
      return compoundsPacker.unpack(v);
    } catch (UnpackInputException e) {
      return null; // this will never happen
    }
  }

  public List<ItemList> getItemLists() {
    return getLists("lists");
  }

  public List<ItemList> getClusteringLists() {
    return getLists("clusterings");
  }

  public List<ItemList> getLists(String name) {
    String value = getItem(name);
    if (value == null) {
      return new ArrayList<ItemList>();
    }
    try {
      return itemListsPacker.unpack(value);
    } catch (UnpackInputException e) {
      return null; // this will never happen
    }
  }

  public List<PackedNetwork> getPackedNetworks() {
    List<PackedNetwork> networks = new ArrayList<PackedNetwork>();
    String value = getItem("networks");
    if (value != null) {
      String[] splits = value.split("###");
      for (String split : splits) {
        String[] subsplits = split.split(":::");
        if (subsplits.length == 2) {
          networks.add(new PackedNetwork(subsplits[0], subsplits[1]));
        }
      }
    }
    return networks;
  }

  public ItemList getGeneSet() {
    return genesetPacker.unpack(getItem("geneset"));
  }

  public void storeCompounds(List<String> compounds) {
    setItem("compounds", compoundsPacker.pack(compounds));
  }

  public void storeColumns(String key, Collection<Group> columns) {
    if (!columns.isEmpty()) {
      SampleColumn first = columns.iterator().next();
      String representative = (first.getSamples().length > 0) ? first.getSamples()[0].toString() : "(no samples)";

      logger.info("Storing columns for " + key + " : " + first + " : " + representative + " ...");
      setItem(key, columnsPacker.pack(columns));
    } else {
      logger.info("Clearing stored columns for: " + key);
      clearItem(key);
    }
  }

  public void storeCustomColumn(Group column) {
    if (column != null) {
      setItem("customColumn", groupPacker.pack(column));
    } else {
      clearItem("customColumn");
    }
  }

  public void storeDatasets(Dataset[] datasets) {
    setItem("datasets", datasetsPacker.pack(Arrays.asList(datasets)));
  }

  public void storeItemLists(List<ItemList> itemLists) {
    setItem("lists", itemListsPacker.pack(itemLists));
  }

  public void storePackedNetworks(List<PackedNetwork> networks) {
    setItem("networks", packedNetworksPacker.pack(networks));
  }

  public void storeSampleClass(SampleClass sampleClass) {
    setItem("sampleClass", sampleClassPacker.pack(sampleClass));
  }

  public void storeProbes(String[] probes) {
    setItem("probes", probesPacker.pack(Arrays.asList(probes)));
  }

  public void storeGeneSet(ItemList geneList) {
    setItem("geneset", (geneList != null ? genesetPacker.pack(geneList) : ""));
  }

  public void storeClusteringLists(List<ItemList> clusteringList) {
    setItem("clusterings", clusteringListsPacker.pack(clusteringList));
  }
}
