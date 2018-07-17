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

package t.viewer.shared;

import java.util.*;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class StringList extends ItemList {

  public final static String PROBES_LIST_TYPE = "probes";
  public final static String COMPOUND_LIST_TYPE = "compounds";

  private String[] items;
  private String comment;

  /**
   * This constructor is here for GWT serialization
   */
  protected StringList() {}

  public StringList(String type, String name, String[] items) {
    super(type, name);
    this.items = items;
  }

  public StringList copyWithName(String name) {
    return new StringList(type, name, items);
  }
  
  public Collection<String> packedItems() {
    return Arrays.asList(items);
  }

  public String[] items() {
    return items;
  }

  public int size() {
    if (items == null) {
      return 0;
    } else {
      return items.length;
    }
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public static List<StringList> pickProbeLists(Collection<? extends ItemList> from,
      @Nullable String title) {
    List<StringList> r = new LinkedList<StringList>();
    for (ItemList l : from) {
      if (l.type().equals(StringList.PROBES_LIST_TYPE) && (title == null || l.name().equals(title))) {
        r.add((StringList) l);
      }
    }
    return r;
  }
  
  @Override
  public String toString() {
    return "StringList:" + pack();
   }
  
  @Override
  public int hashCode() {
    return name.hashCode() + 41 * (
        type.hashCode() + 41 * (
            Arrays.hashCode(items())
            ));
  }
  
  @Override
  public boolean equals(Object other) {
    if (! (other instanceof StringList) || other == null) {
      return false;
    }
    StringList sol = (StringList) other;
    return sol.name().equals(name()) &&
        sol.type().equals(type()) &&
        Arrays.equals(items(), sol.items());
        
  }
}
