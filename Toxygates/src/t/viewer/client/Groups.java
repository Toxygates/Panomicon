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
import java.util.stream.Collectors;

import t.common.shared.DataSchema;
import t.common.shared.sample.*;
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
  
  private List<ClientGroup> getGroups(boolean active) {
    return storage.allObjects().stream().filter(g -> g.active == active).collect(Collectors.toList());
  }
  
  public List<ClientGroup> activeGroups() {
    return getGroups(true);
  }
  
  public List<ClientGroup> inactiveGroups() {
    return getGroups(false);
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
  
  public String nextColor() {
    HashMap<String, Integer> existingColors = new HashMap<String, Integer>();
    // count the number of uses of each color
    for (ClientGroup group : allGroups()) {
      String color = group.getColor();
      if (existingColors.containsKey(color)) {
        existingColors.put(color, existingColors.get(color) + 1);
      } else {
        existingColors.put(color, 1);
      }
    }
    // find the least frequently used color that is first on the list of colors
    String bestColorSoFar = SampleGroup.groupColors[0]; 
    int smallestCountSoFar = Integer.MAX_VALUE;
    for (String color : SampleGroup.groupColors) {
      Integer newColorCount = existingColors.getOrDefault(color, 0);
      if (newColorCount < smallestCountSoFar) {
        smallestCountSoFar = newColorCount;
        bestColorSoFar = color;
      }
    }
    return bestColorSoFar;
  }

  private String firstChars(String s) {
    if (s.length() < 8) {
      return s;
    } else {
      return s.substring(0, 8);
    }
  }
}
