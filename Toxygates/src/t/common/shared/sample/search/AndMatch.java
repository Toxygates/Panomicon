/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package t.common.shared.sample.search;

import java.io.Serializable;
import java.util.*;

import t.model.sample.Attribute;

@SuppressWarnings("serial")
public class AndMatch implements MatchCondition, Serializable {
  Collection<? extends MatchCondition> conditions;
  
  //GWT constructor
  public AndMatch() {}
  
  public AndMatch(Collection<? extends MatchCondition> conditions) {
    this.conditions = conditions;
  }

  @Override
  public Collection<Attribute> neededParameters() {
    Set<Attribute> r = new HashSet<Attribute>();
    for (MatchCondition mc: conditions) {
      r.addAll(mc.neededParameters());
    }
    return r;
  }
  
  public Collection<? extends MatchCondition> subConditions() { return conditions; }
}
