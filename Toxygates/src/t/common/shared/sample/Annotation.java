/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.shared.sample;

import java.io.Serializable;
import java.util.List;

/**
 * A set of annotations (such as chemical data and morphological data) corresponding to a microarray
 * sample.
 */
@SuppressWarnings("serial")
public class Annotation implements Serializable {
  public Annotation(String id, List<Entry> annotations) {
    _id = id;
    _entries = annotations;
  }

  public Annotation() {}

  private String _id;

  public String id() {
    return _id;
  }

  private List<Entry> _entries;

  /**
   * An entry has a string value and/or a numerical value. The user needs to track which value has
   * been set in some external way.
   */
  public static class Entry implements Serializable {
    public String description;
    public String value;
    public boolean numerical;

    public Entry() {}

    public Entry(String description, String value, boolean numerical) {
      this.description = description;
      this.value = value;
      this.numerical = numerical;
    }
  }

  public List<Entry> getEntries() {
    return _entries;
  }

  public double doubleValueFor(String entryName) throws Exception {
    for (Annotation.Entry e : getEntries()) {
      if (e.description.equals(entryName)) {
        return Double.valueOf(e.value);
      }
    }
    throw new Exception("Value not available");
  }
}
