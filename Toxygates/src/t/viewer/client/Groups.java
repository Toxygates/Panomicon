package t.viewer.client;

import java.util.*;
import java.util.stream.Collectors;

import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.common.shared.sample.Unit;
import t.viewer.client.storage.NamedObjectStorage;
import t.viewer.client.storage.Storage;

/**
 * Functionality for managing active and inactive groups.
 */
public class Groups {
  private NamedObjectStorage<ClientGroup> storage;
  
  //private Logger logger = SharedUtils.getLogger("groups");
  
  public Groups(Storage<List<ClientGroup>> groupsStorage) {
    this.storage = new NamedObjectStorage<ClientGroup>(groupsStorage, g ->  g.getName());
  }
  
  public NamedObjectStorage<ClientGroup> storage() {
    return storage;
  }
  
  public List<ClientGroup> activeGroups() {
    return storage.allObjects().stream().filter(g -> g.active).collect(Collectors.toList());
  }

  public void put(ClientGroup value) {
    storage.put(value.getName(), value);
  }

  public void deactivate(Group group) {
    storage.get(group.getName()).active = false;
  }

  public void setActiveGroups(Collection<ClientGroup> selection) {
    for (ClientGroup g : storage.allObjects()) {
      g.active = false;
    }
    for (Group g : selection) {
      storage.get(g.getName()).active = true;
    }
  }

  public Collection<ClientGroup> allGroups() {
    List<ClientGroup> groups = storage.allObjects(); 
    Collections.sort(groups);
    return groups;
  }

  public int size() {
    return storage.size();
  }

  public void clear() {
    storage.clear();
  }
  
  public String suggestName(List<Unit> units, DataSchema schema) {
    String groupDescription = "";
    if (!units.isEmpty()) {
      Unit firstUnit = units.get(0);
      groupDescription = firstChars(firstUnit.get(schema.majorParameter())) + "/"
          + firstUnit.get(schema.mediumParameter()).substring(0, 1) + "/" + firstUnit.get(schema.minorParameter());
      if (units.size() > 1) {
        groupDescription += ", ...";
      }
    } else {
      groupDescription = "Empty group";
    }
    return storage.suggestName(groupDescription);
  }

  private String firstChars(String s) {
    if (s.length() < 8) {
      return s;
    } else {
      return s.substring(0, 8);
    }
  }
}
