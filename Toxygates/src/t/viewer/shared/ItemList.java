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

package t.viewer.shared;

import java.io.Serializable;
import java.util.Collection;

/**
 * A typed, named list of items.
 * 
 * Current supported types are "probes" and "compounds". However, lists of type probes may actually
 * be gene identifiers (entrez).
 */
@SuppressWarnings("serial")
abstract public class ItemList implements Serializable, Comparable<ItemList> {

  protected String type;
  protected String name;

  protected ItemList() {}

  public ItemList(String type, String name) {
    this.name = name;
    this.type = type;
  }

  public String name() {
    return name;
  }

  public String type() {
    return type;
  }

  abstract public Collection<String> packedItems();

  abstract public int size();

  @Override
  public int compareTo(ItemList o) {
    return name().compareTo(o.name());
  }


}
