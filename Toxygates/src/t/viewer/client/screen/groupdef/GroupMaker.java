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

package t.viewer.client.screen.groupdef;

import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.sample.Unit;
import t.model.sample.Attribute;
import t.viewer.client.ClientGroup;
import t.viewer.client.Groups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupMaker {

  /**
   * Generate suitable groups, one per "major" value, from the given units. This algorithm simply
   * finds the dose/time combination with the largest number of majors available, and then creates
   * groups with 1 unit each.
   */
  public static void autoGroups(GroupInspector groupInspector, DataSchema schema,
                                List<Pair<Unit, Unit>> units, Groups groups) {
    Map<String, List<Pair<Unit, Unit>>> byMedMin = new HashMap<String, List<Pair<Unit, Unit>>>();

    final Attribute medParam = schema.mediumParameter(), minParam = schema.minorParameter();

    int maxLen = 0;
    String maxKey = "";

    for (Pair<Unit, Unit> p : units) {
      Unit u = p.first();
      String medMin = u.get(medParam) + u.get(minParam);

      if (!byMedMin.containsKey(medMin)) {
        byMedMin.put(medMin, new ArrayList<Pair<Unit, Unit>>());
      }
      byMedMin.get(medMin).add(p);

      int len = byMedMin.get(medMin).size();
      if (len > maxLen) {
        maxLen = len;
        maxKey = medMin;
      }
    }

    for (Pair<Unit, Unit> p : byMedMin.get(maxKey)) {
      List<Unit> us = new ArrayList<Unit>();
      us.add(p.first());
      Unit c = p.second();
      if (c != null) {
        us.add(c);
      }
      String n = groupInspector.groups.suggestName(us, schema);
      ClientGroup g = new ClientGroup(schema, n, us.toArray(new Unit[0]), true, groups.nextColor());
      groups.put(g);
    }
  }
}
