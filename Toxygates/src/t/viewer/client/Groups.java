package t.viewer.client;

import java.util.*;

import com.google.gwt.user.client.Window;

import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.common.shared.sample.Unit;
import t.model.sample.AttributeSet;

public class Groups {
  private Map<String, Group> groups = new HashMap<String, Group>();
  private List<Group> allGroups = new ArrayList<Group>();
  public List<Group> chosenColumns = new ArrayList<Group>();

  public void loadGroups(StorageParser parser, DataSchema schema, AttributeSet attributes) {
    clear();

    // Load chosen columns
    chosenColumns = parser.getChosenColumns(schema, attributes);
    for (Group g : chosenColumns) {
      groups.put(g.getName(), g);
    }
    //updateConfigureStatus(false);
    allGroups.addAll(sortedGroupList(chosenColumns));

    // Load inactive columns
    Collection<Group> inactiveGroups = null;
    try {
      List<Group> inactiveColumns = parser.getColumns(schema, "inactiveColumns", attributes);
      inactiveGroups = sortedGroupList(inactiveColumns);
      for (Group g : inactiveGroups) {
        groups.put(g.getName(), g);
      }
      allGroups.addAll(sortedGroupList(inactiveGroups));
    } catch (Exception e) {
      //logger.log(Level.WARNING, "Unable to load inactive columns", e);
      Window.alert("Unable to load inactive columns.");
    }
  }

  public List<Group> all() {
    return allGroups;
  }

  public List<Group> chosen() {
    return chosenColumns;
  }

  public Group get(String key) {
    return groups.get(key);
  }

  public void put(String key, Group value) {
    groups.put(key, value);
  }

  public boolean containsKey(String key) {
    return groups.containsKey(key);
  }

  public void remove(String key) {
    groups.remove(key);
  }

  public Collection<Group> values() {
    return groups.values();
  }

  public int size() {
    return groups.size();
  }

  public void clear() {
    groups.clear();
    allGroups.clear();
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
