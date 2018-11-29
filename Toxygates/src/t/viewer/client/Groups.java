package t.viewer.client;

import java.util.*;

import com.google.gwt.user.client.Window;

import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.common.shared.sample.Unit;
import t.model.sample.AttributeSet;

/**
 * Functionality for managing active and inactive groups, factored out of
 * GroupInspector. The logic for the most part comes directly from
 * GroupInspector, and so is in need of significant further refactoring.
 */
public class Groups {
  private Map<String, Group> groups = new HashMap<String, Group>();
  private List<Group> activeGroups = new ArrayList<Group>();

  public void loadGroups(StorageParser parser, DataSchema schema, AttributeSet attributes) {
    clear();

    // Load chosen columns
    activeGroups = parser.getChosenColumns(schema, attributes);
    for (Group g : activeGroups) {
      groups.put(g.getName(), g);
    }
    //allGroups.addAll(sortedGroupList(chosenColumns));

    // Load inactive columns
    Collection<Group> inactiveGroups = null;
    try {
      List<Group> inactiveColumns = parser.getColumns(schema, "inactiveColumns", attributes);
      inactiveGroups = sortedGroupList(inactiveColumns);
      for (Group g : inactiveGroups) {
        groups.put(g.getName(), g);
      }
      //allGroups.addAll(sortedGroupList(inactiveGroups));
    } catch (Exception e) {
      //logger.log(Level.WARNING, "Unable to load inactive columns", e);
      Window.alert("Unable to load inactive columns.");
    }
  }

  public void saveToLocalStorage(StorageParser parser) {
    parser.storeColumns("columns", activeGroups());

    List<Group> inactiveGroups = new ArrayList<Group>(groups.values());
    inactiveGroups.removeAll(activeGroups());
    parser.storeColumns("inactiveColumns", inactiveGroups);
  }

  public List<Group> activeGroups() {
    return activeGroups;
  }

  public Group get(String key) {
    return groups.get(key);
  }

  public void put(String key, Group value, boolean chosen) {
    groups.put(key, value);
    if (chosen) {
      activeGroups.add(value);
    }
  }

  public boolean containsKey(String key) {
    return groups.containsKey(key);
  }

  public void remove(String key) {
    Group removed = groups.remove(key);
    if (removed != null) {
      deactivate(removed);
    }
  }

  public void deactivate(Group group) {
    activeGroups.remove(group);
  }

  public void setActive(Collection<Group> selection) {
    activeGroups = new ArrayList<Group>(selection);
  }

  public Collection<Group> allGroups() {
    return groups.values();
  }

  public int size() {
    return groups.size();
  }

  public void clear() {
    groups.clear();
    activeGroups.clear();
  }

  private List<Group> sortedGroupList(Collection<Group> groups) {
    ArrayList<Group> r = new ArrayList<Group>(groups);
    Collections.sort(r);
    return r;
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
    int i = 1;
    String name = groupDescription;
    while (groups.containsKey(name)) {
      name = groupDescription + " " + i;
      i++;
    }
    return name;
  }

  private String firstChars(String s) {
    if (s.length() < 8) {
      return s;
    } else {
      return s.substring(0, 8);
    }
  }
}
