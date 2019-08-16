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

package t.viewer.client.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;

import t.common.shared.*;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.viewer.client.network.PackedNetwork;
import t.viewer.shared.AppInfo;
import t.viewer.shared.ItemList;
import t.viewer.shared.mirna.MirnaSource;

/**
 * Refactoring in progress. Methods for converting objects to and from strings
 * have been moved to Packer.
 */
public class StorageProvider implements Storage.StorageProvider {

  private final String prefix;
  private final com.google.gwt.storage.client.Storage storage;
  private static final char[] reservedChars = new char[] {':', '#', '$', '^'};
  public static final String unacceptableStringMessage =
      "The characters ':', '#', '$' and '^' are reserved and may not be used.";

  protected static final Logger logger = SharedUtils.getLogger("storage");

  // These Packer and Storage instances are initialized in the constructor
  public final SampleClassPacker sampleClassPacker;
  public final SamplePacker samplePacker;
  public final GroupPacker groupPacker;
  public final ListPacker<Group> columnsPacker;
  public final Storage<SampleClass> sampleClassStorage;
  public final Storage<List<Group>> chosenColumnsStorage;
  public final Storage<List<Group>> inactiveColumnsStorage;
  public final Storage<Group> customColumnStorage;
  public final Storage<List<Dataset>> datasetsStorage;
  
  public final ListPacker<String> stringListPacker = 
      new ListPacker<String>(new IdentityPacker(), "###");
  
  public final ListPacker<Dataset> datasetsPacker = 
      new ListPacker<Dataset>(new DatasetPacker(), "###");
  
  public final ListPacker<ItemList> itemListsPacker = 
      new ListPacker<ItemList>(new ItemListPacker(), "###");
  
  public final ItemListPacker genesetPacker = new ItemListPacker();
  
  public final ListPacker<PackedNetwork> packedNetworksPacker = 
      new ListPacker<PackedNetwork>(new PackedNetworkPacker(), "###");
  
  public final ListPacker<MirnaSource> mirnaSourcesPacker = 
      new ListPacker<MirnaSource>(new MirnaSourcePacker(), ":::");
  
  public final Storage<List<String>> probesStorage = 
      new Storage<List<String>>("probes", stringListPacker, this, 
          () -> new ArrayList<String>());
  
  public final Storage<List<String>> compoundsStorage = 
      new Storage<List<String>>("compounds", stringListPacker, this, 
          () -> new ArrayList<String>());
  
  public final Storage<List<ItemList>> itemListsStorage = 
      new Storage<List<ItemList>>("lists", itemListsPacker, this, 
          () -> new ArrayList<ItemList>());
  
  public final Storage<List<ItemList>> clusteringListsStorage = 
      new Storage<List<ItemList>>("clusterings", itemListsPacker, this, 
          () -> new ArrayList<ItemList>());

  public final Storage<ItemList> genesetStorage = 
      new Storage<ItemList>("geneset", genesetPacker, this);
  
  public final Storage<List<PackedNetwork>> packedNetworksStorage = 
      new Storage<List<PackedNetwork>>("networks", packedNetworksPacker, this,
          () -> new ArrayList<PackedNetwork>());
  
  public final Storage<List<String>> columnStateStorage = 
      new Storage<List<String>>("hideableColumns", stringListPacker, this,
          () -> new ArrayList<String>());
  
  public final Storage<List<MirnaSource>> mirnaSourcesStorage = 
      new Storage<List<MirnaSource>>("mirnaSources", mirnaSourcesPacker, this);
  
  public StorageProvider(com.google.gwt.storage.client.Storage storage, String prefix, 
      DataSchema schema, AppInfo info) {
    
    this.prefix = prefix;
    this.storage = storage;

    sampleClassPacker = new SampleClassPacker(info.attributes());
    samplePacker = new SamplePacker(sampleClassPacker);
    groupPacker = new GroupPacker(samplePacker, schema);
    columnsPacker = new ListPacker<Group>(groupPacker, "###");
    
    sampleClassStorage = 
        new Storage<SampleClass>("sampleClass", sampleClassPacker, this, () -> new SampleClass());
    chosenColumnsStorage = 
        new Storage<List<Group>>("columns", columnsPacker, this,
            () -> new ArrayList<Group>());
    inactiveColumnsStorage = 
        new Storage<List<Group>>("inactiveColumns", columnsPacker, this, 
            () -> new ArrayList<Group>());
    
    customColumnStorage = new Storage<Group>("customColumn", groupPacker, this);
    
    datasetsStorage = new Storage<List<Dataset>>("datasets", datasetsPacker, this,
        () -> Dataset.defaultSelection(info.datasets()));
  }

  @Override
  public void setItem(String key, String value) {
    storage.setItem(prefix + "." + key, value);
    // logger.info("SET " + prefix + "." + key + " -> " + value);
  }

  @Override
  public String getItem(String key) {
    String v = storage.getItem(prefix + "." + key);
    // logger.info("GET " + prefix + "." + key + " -> " + v);
    return v;
  }

  @Override
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
  
  public List<Group> getChosenColumns() {
    return chosenColumnsStorage.getWithExceptionHandler(e -> 
        logger.log(Level.WARNING, "Exception while retrieving columns", e));
  }

  public List<Group> getInactiveColumns() {
    return inactiveColumnsStorage.getWithExceptionHandler(e -> 
        logger.log(Level.WARNING, "Exception while retrieving columns", e));
  }
}
