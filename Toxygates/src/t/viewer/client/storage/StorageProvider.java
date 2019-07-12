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

package t.viewer.client.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.user.client.Window;

import t.common.shared.*;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.viewer.client.ClientGroup;
import t.viewer.client.network.PackedNetwork;
import t.viewer.shared.AppInfo;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
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
  public final Storage<Group> customColumnStorage;
  public final Storage<List<Dataset>> datasetsStorage;
  
  // These two are private because they are obsolete; should only be accessed
  // when groupsStorage fails
  private final Storage<List<Group>> chosenColumnsStorage;
  private final Storage<List<Group>> inactiveColumnsStorage;
  
  
  public final ListPacker<String> stringsPacker = 
      new ListPacker<String>(new IdentityPacker(), "###");
  
  public final ListPacker<Dataset> datasetsPacker = 
      new ListPacker<Dataset>(new DatasetPacker(), "###");
  
  public final ListPacker<ItemList> itemListsPacker = 
      new ListPacker<ItemList>(new ItemListPacker(), "###");
  
  public final ListPacker<StringList> stringListsPacker = 
      new ListPacker<StringList>(new StringListPacker(), "###");
  
  public final ItemListPacker genesetPacker = new ItemListPacker();
  
  public final ListPacker<PackedNetwork> packedNetworksPacker = 
      new ListPacker<PackedNetwork>(new PackedNetworkPacker(), "###");
  
  public final ListPacker<MirnaSource> mirnaSourcesPacker = 
      new ListPacker<MirnaSource>(new MirnaSourcePacker(), ":::");
  
  public final ListPacker<ClientGroup> clientGroupsPacker;
  
  public final Storage<List<String>> probesStorage = 
      new Storage<List<String>>("probes", stringsPacker, this, 
          () -> new ArrayList<String>());
  
  public final Storage<List<String>> compoundsStorage = 
      new Storage<List<String>>("compounds", stringsPacker, this, 
          () -> new ArrayList<String>());
  
  public final Storage<List<ItemList>> itemListsStorage = 
      new Storage<List<ItemList>>("lists", itemListsPacker, this, 
          () -> new ArrayList<ItemList>());

  public final Storage<List<StringList>> geneSetsStorage = 
      new Storage<List<StringList>>("geneSets", stringListsPacker, this, 
          () -> {
            // Fallback: get all the gene sets from itemListsStorage
            return itemListsStorage.getIgnoringException().stream().
              filter(l -> l.type() == StringList.PROBES_LIST_TYPE).
              map(l -> (StringList) l).collect(Collectors.toList());
          });
  
  public final Storage<List<StringList>> compoundListStorage = 
      new Storage<List<StringList>>("compoundLists", stringListsPacker, this, 
          () -> {
            // Fallback: get all the compound lists from itemListsStorage
            return itemListsStorage.getIgnoringException().stream().
              filter(l -> l.type() == StringList.COMPOUND_LIST_TYPE).
              map(l -> (StringList) l).collect(Collectors.toList());
          });
  
  public final Storage<List<ItemList>> clusteringListsStorage = 
      new Storage<List<ItemList>>("clusterings", itemListsPacker, this, 
          () -> new ArrayList<ItemList>());

  public final Storage<ItemList> chosenGenesetStorage = 
      new Storage<ItemList>("geneset", genesetPacker, this);
  
  public final Storage<List<PackedNetwork>> packedNetworksStorage = 
      new Storage<List<PackedNetwork>>("networks", packedNetworksPacker, this,
          () -> new ArrayList<PackedNetwork>());
  
  public final Storage<List<String>> columnStateStorage = 
      new Storage<List<String>>("hideableColumns", stringsPacker, this,
          () -> new ArrayList<String>());
  
  public final Storage<List<MirnaSource>> mirnaSourcesStorage = 
      new Storage<List<MirnaSource>>("mirnaSources", mirnaSourcesPacker, this);
  
  public final Storage<List<ClientGroup>> groupsStorage;
  
  public StorageProvider(com.google.gwt.storage.client.Storage storage, String prefix, 
      DataSchema schema, AppInfo info) {
    
    this.prefix = prefix;
    this.storage = storage;

    sampleClassPacker = new SampleClassPacker(info.attributes());
    samplePacker = new SamplePacker(sampleClassPacker);
    groupPacker = new GroupPacker(samplePacker, schema);
    clientGroupsPacker =
        new ListPacker<ClientGroup>(new ClientGroupPacker(samplePacker, schema), "###");
    columnsPacker = new ListPacker<Group>(groupPacker, "###");
    
    sampleClassStorage = 
        new Storage<SampleClass>("sampleClass", sampleClassPacker, this, () -> new SampleClass());
    
    // These two are obsolete now that we are using groupsStorage instead, but  
    // we are keeping them around so we can load users' old data.
    chosenColumnsStorage = 
        new Storage<List<Group>>("columns", columnsPacker, this,
            () -> new ArrayList<Group>());
    inactiveColumnsStorage = 
        new Storage<List<Group>>("inactiveColumns", columnsPacker, this, 
            () -> new ArrayList<Group>());
    
    groupsStorage = new Storage<List<ClientGroup>>("groups", clientGroupsPacker, this,
        () -> recoverOldGroups());
    
    customColumnStorage = new Storage<Group>("customColumn", groupPacker, this);
    
    datasetsStorage = new Storage<List<Dataset>>("datasets", datasetsPacker, this,
        () -> Dataset.defaultSelection(info.datasets()));
  }
  
  /**
   * Loads groups from the old chosenColumns/inactiveColumns storage and converts 
   * them to ClientGroups.
   * @return a list of groups, represented as ClientGroups
   */
  private List<ClientGroup> recoverOldGroups() {
    List<Group> activeGroups = chosenColumnsStorage.getIgnoringException();
    List<Group> inactiveGroups = inactiveColumnsStorage.getIgnoringException();
    
    Stream<ClientGroup> activeGroupsStream = 
        activeGroups.stream().map(group -> new ClientGroup(group, true));
    Stream<ClientGroup> inactiveGroupsStream = 
        inactiveGroups.stream().map(group -> new ClientGroup(group, false));
    return Stream.concat(activeGroupsStream, inactiveGroupsStream).collect(Collectors.toList());
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
}
