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

package t.viewer.client;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Group;
import t.common.shared.sample.Unit;
import t.viewer.client.storage.StorageProvider;

/**
 * Functionality for managing active and inactive groups, factored out of
 * GroupInspector. The logic for the most part comes directly from
 * GroupInspector, and so is in need of significant further refactoring.
 */
public class Groups {
  private Map<String, Group> groups = new LinkedHashMap<String, Group>();
  private List<Group> activeGroups = new ArrayList<Group>();
  private Logger logger = SharedUtils.getLogger("groups");

  public void loadGroups(StorageProvider storage) {
    clear();

    // Load chosen columns
    activeGroups = sortedGroupList(storage.getChosenColumns());
    for (Group g : activeGroups) {
      groups.put(g.getName(), g);
    }

    // Load inactive columns
    try {
      Collection<Group> inactiveGroups = sortedGroupList(storage.getInactiveColumns());
      for (Group g : inactiveGroups) {
        groups.put(g.getName(), g);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to load inactive columns", e);
      Window.alert("Unable to load inactive columns.");
    }
  }

  public void saveToLocalStorage(StorageProvider storage) {
    storage.chosenColumnsStorage.store(activeGroups());

    List<Group> inactiveGroups = new ArrayList<Group>(groups.values());
    inactiveGroups.removeAll(activeGroups());
    storage.inactiveColumnsStorage.store(inactiveGroups);
  }

  public List<Group> activeGroups() {
    return activeGroups;
  }

  public Group get(String key) {
    return groups.get(key);
  }

  public void put(String key, Group value, boolean chosen) {
    Group oldGroup = groups.get(key);
    if (oldGroup != null) {
      activeGroups.remove(oldGroup);
    }
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

  public void setActiveGroups(Collection<Group> selection) {
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
