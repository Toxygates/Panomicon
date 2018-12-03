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

import t.common.client.Utils;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.network.PackedNetwork;
import t.viewer.shared.AppInfo;
import t.viewer.shared.ItemList;

/**
 * Storage parsing/serialising code. Some is still spread out in other classes, 
 * such as Group.
 * PersistedState may also be considered for some of these items in the future, where 
 * lifecycle management is needed.
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

  public static String packSample(Sample sample) {
    final String sep = "$$$";
    StringBuilder sb = new StringBuilder();
    sb.append("Barcode_v3").append(sep);
    sb.append(sample.id()).append(sep);
    sb.append(Utils.packSampleClass(sample.sampleClass())).append(sep);
    return sb.toString();
  }

  public static @Nullable Sample unpackSample(String s, AttributeSet attributeSet) {
    String[] spl = s.split("\\$\\$\\$");
    String v = spl[0];
    if (!v.equals("Barcode_v3")) {
      Window.alert("Legacy data has been detected in your browser's storage. "
          + "Some of your older sample groups may not load properly.");
      return null;
    }
    String id = spl[1];
    SampleClass sc = Utils.unpackSampleClass(attributeSet, spl[2]);
    return new Sample(id, sc);
  }

  public static String packColumns(Collection<? extends SampleColumn> columns) {
    return packPackableList(columns, "###");
  }

  @Nullable
  public static Group unpackColumn(DataSchema schema, String s, AttributeSet attributes) {
    if (s == null) {
      return null;
    }
    String[] spl = s.split("\\$\\$\\$");
    if (!spl[0].equals("Barcode") && !spl[0].equals("Barcode_v3")) {
      return Group.unpack(schema, s, attributes);
    } else {
      // Legacy or incorrect format
      logger.warning("Unexpected column format: " + s);
      return null;
    }
  }

  public static String packProbes(String[] probes) {
    return packList(Arrays.asList(probes), "###");
  }

  public static String packPackableList(Collection<? extends Packable> items, String separator) {
    List<String> xs = new ArrayList<String>();
    for (Packable p : items) {
      xs.add(p.pack());
    }
    return packList(xs, separator);
  }

  public static String packList(Collection<String> items, String separator) {
    // TODO best location of this? handle viewer/common separation cleanly.
    return SharedUtils.packList(items, separator);
  }

  public static String packItemLists(Collection<ItemList> lists, String separator) {
    return packPackableList(lists, separator);
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
      return Utils.unpackSampleClass(attributes, v);
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
        Group c = unpackColumn(schema, cl, attributes);
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
    return unpackColumn(schema, getItem("customColumn"), attributes);
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

  public static String packDatasets(Dataset[] datasets) {
    List<String> r = new ArrayList<String>();
    for (Dataset d : datasets) {
      r.add(d.getId());
    }
    return packList(r, "###");
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

  public String packCompounds(List<String> compounds) {
    return packList(compounds, "###");
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
    setItem("compounds", packList(compounds, "###"));
  }

  public void storeColumns(String key, Collection<? extends SampleColumn> columns) {
    if (!columns.isEmpty()) {
      SampleColumn first = columns.iterator().next();
      String representative = (first.getSamples().length > 0) ? first.getSamples()[0].toString() : "(no samples)";

      logger.info("Storing columns for " + key + " : " + first + " : " + representative + " ...");
      setItem(key, packColumns(columns));
    } else {
      logger.info("Clearing stored columns for: " + key);
      clearItem(key);
    }
  }

  public void storeCustomColumn(DataColumn<?> column) {
    if (column != null) {
      setItem("customColumn", column.pack());
    } else {
      clearItem("customColumn");
    }
  }

  public void storeDatasets(Dataset[] datasets) {
    setItem("datasets", StorageParser.packDatasets(datasets));
  }

  public void storeItemLists(List<ItemList> itemLists) {
    setItem("lists", packItemLists(itemLists, "###"));
  }

  public void storePackedNetworks(List<PackedNetwork> networks) {
    List<String> networkStrings = new ArrayList<String>();
    for (PackedNetwork network : networks) {
      networkStrings.add(network.title() + ":::" + network.jsonString());
    }
    setItem("networks", packList(networkStrings, "###"));
  }

  public void storeSampleClass(SampleClass sampleClass) {
    setItem("sampleClass", t.common.client.Utils.packSampleClass(sampleClass));
  }

  public void storeProbes(String[] probes) {
    setItem("probes", packProbes(probes));
  }

  public void storeGeneSet(ItemList geneList) {
    setItem("geneset", (geneList != null ? geneList.pack() : ""));
  }

  public void storeClusteringLists(List<ItemList> clusteringList) {
    setItem("clusterings", packItemLists(clusteringList, "###"));
  }
}
