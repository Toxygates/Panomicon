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

import javax.annotation.Nullable;

import t.model.sample.Attribute;

@SuppressWarnings("serial")
public class AtomicMatch implements MatchCondition, Serializable {
  @Nullable Double param1;
  Attribute attr;
  MatchType matchType;
  
  //GWT constructor
  public AtomicMatch() {}
  
  /**
   * @param paramId parameter (the value of the parameter is irrelevant)
   * @param matchType
   * @param param1
   */
  public AtomicMatch(Attribute paramId, MatchType matchType,
      @Nullable Double param1) {
    this.matchType = matchType;
    this.attr = paramId;
    this.param1 = param1;
  }
  
  @Override
  public Collection<Attribute> neededParameters() {
    List<Attribute> r = new ArrayList<Attribute>();
    r.add(attr);        
    return r;
  }
  
  public MatchType matchType() { return matchType; }
  
  public @Nullable Double param1() { return param1; }
  
  /**
   * @return parameter identified by human-readable string
   */
  public Attribute parameter() { return attr; }
}
