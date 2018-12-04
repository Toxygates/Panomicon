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
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
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

  public StorageParser(Storage storage, String prefix) {
    this.prefix = prefix;
    this.storage = storage;
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

  @Nullable
  public SampleClass getSampleClass(AttributeSet attributes) {
    String v = getItem("sampleClass");
    if (v == null) {
      return new SampleClass();
    } else {
      return Packer.unpackSampleClass(attributes, v);
    }
  }

  @Nullable
  // Separator hierarchy for columns:
  // ### > ::: > ^^^ > $$$
  public List<Group> getColumns(DataSchema schema, String key, AttributeSet attributes) throws Exception {
    String v = getItem(key);
    List<Group> r = new ArrayList<Group>();
    if (v != null) {
      String[] spl = v.split("###");
      for (String cl : spl) {
        Group c = Packer.unpackColumn(schema, cl, attributes);
        r.add(c);
      }
    }
    return r;
  }

  public List<Group> getChosenColumns(DataSchema schema, AttributeSet attributes) {
    try {
      return getColumns(schema, "columns", attributes);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Exception while retrieving columns", e);
      return new ArrayList<Group>();
    }
  }

  public Group getCustomColumn(DataSchema schema, AttributeSet attributes) {
    return Packer.unpackColumn(schema, getItem("customColumn"), attributes);
  }

  public String[] getProbes() {
    String probeString = getItem("probes");
    if (probeString != null && !probeString.equals("")) {
      return probeString.split("###");
    } else {
      return new String[0];
    }
  }

  public Dataset[] getDatasets(AppInfo info) {
    String v = getItem("datasets");
    if (v == null) {      
      return Dataset.defaultSelection(info.datasets());        
    }
    List<Dataset> r = new ArrayList<Dataset>();
    for (String ds : v.split("###")) {
      r.add(new Dataset(ds, "", "", null, ds, 0));
    }
    return r.toArray(new Dataset[0]);
  }

  public List<String> getCompounds() {
    String v = getItem("compounds");
    if (v == null) {
      return new ArrayList<String>();
    }
    List<String> r = new ArrayList<String>();
    if (v.length() > 0) {
      for (String c : v.split("###")) {
        r.add(c);
      }
    }
    return r;
  }

  public List<ItemList> getItemLists() {
    return getLists("lists");
  }

  public List<ItemList> getClusteringLists() {
    return getLists("clusterings");
  }

  public List<ItemList> getLists(String name) {
    List<ItemList> r = new ArrayList<ItemList>();
    String v = getItem(name);
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
    return ItemList.unpack(getItem("geneset"));
  }

  public void storeCompounds(List<String> compounds) {
    setItem("compounds", Packer.packList(compounds, "###"));
  }

  public void storeColumns(String key, Collection<Group> columns) {
    if (!columns.isEmpty()) {
      SampleColumn first = columns.iterator().next();
      String representative = (first.getSamples().length > 0) ? first.getSamples()[0].toString() : "(no samples)";

      logger.info("Storing columns for " + key + " : " + first + " : " + representative + " ...");
      setItem(key, Packer.packColumns(columns));
    } else {
      logger.info("Clearing stored columns for: " + key);
      clearItem(key);
    }
  }

  public void storeCustomColumn(Group column) {
    if (column != null) {
      setItem("customColumn", Packer.packGroup(column));
    } else {
      clearItem("customColumn");
    }
  }

  public void storeDatasets(Dataset[] datasets) {
    setItem("datasets", Packer.packDatasets(datasets));
  }

  public void storeItemLists(List<ItemList> itemLists) {
    setItem("lists", Packer.packItemLists(itemLists, "###"));
  }

  public void storePackedNetworks(List<PackedNetwork> networks) {
    List<String> networkStrings = new ArrayList<String>();
    for (PackedNetwork network : networks) {
      networkStrings.add(network.title() + ":::" + network.jsonString());
    }
    setItem("networks", Packer.packList(networkStrings, "###"));
  }

  public void storeSampleClass(SampleClass sampleClass) {
    setItem("sampleClass", Packer.packSampleClass(sampleClass));
  }

  public void storeProbes(String[] probes) {
    setItem("probes", Packer.packProbes(probes));
  }

  public void storeGeneSet(ItemList geneList) {
    setItem("geneset", (geneList != null ? geneList.pack() : ""));
  }

  public void storeClusteringLists(List<ItemList> clusteringList) {
    setItem("clusterings", Packer.packItemLists(clusteringList, "###"));
  }
}
