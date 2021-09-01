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

import t.shared.common.AType;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An association is a mapping from probes to other objects. They are used as "dynamic columns" in
 * the GUI. The mapped-to objects have names and formal identifiers.
 */
@SuppressWarnings("serial")
public class Association implements Serializable {

  private AType _type;
  private Map<String, ? extends Set<AssociationValue>> _data =
      new HashMap<String, HashSet<AssociationValue>>();
  private int  _sizeLimit;
  private boolean _anyOverSizeLimit;
  private boolean _success;

  public Association() {}

  /**
   * Construct an Association result with data.
   * @param type
   * @param data Association data keyed on probe id:s.
   * @param sizeLimit A limit on the number of associations to return for
   *                  each probe. The results in the data wil be truncated
   *                  based on this limit. Note: if >n associations are found
   *                  for a probe, then n+1 will be kept, in order to provide an
   *                  indication that >n results were found.
   */
  public Association(AType type, Map<String, ? extends Set<AssociationValue>> data,
                     int sizeLimit, boolean success) {
    this(type, success);

    // Go through the data and truncate the values so there are no more than sizeLimit+1
    // for each probe
    Map<String, ? extends Set<AssociationValue>> newData = data.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
      entry -> {
        Set<AssociationValue> values = entry.getValue();
        if (values.size() > sizeLimit) {
          _anyOverSizeLimit = true;
        }
        int newSize = Math.min(sizeLimit + 1, values.size());
        AssociationValue[] valuesArray = values.toArray(new AssociationValue[newSize]);
        AssociationValue[] newValuesArray = Arrays.copyOfRange(valuesArray, 0, newSize);
        return new HashSet<AssociationValue>(Arrays.asList(newValuesArray));
      }));
    _sizeLimit = sizeLimit;
    _data = newData;
  }
  
  /**
   * Construct an empty Association result.
   * @param type
   */
  public Association(AType type, boolean success) {
    _type = type;
    _success = success;
  }

  public AType type() {
    return _type;
  }

  public String title() {
    return _type.name();
  }

  public Map<String, ? extends Set<AssociationValue>> data() {
    return _data;
  }
  
  public int sizeLimit() {
    return _sizeLimit;
  }

  /**
   * @return true iff the data provided when creating this Association had more
   *         results than _sizeLimit for at least one probe.
   */
  public boolean anyOverSizeLimit() { return _anyOverSizeLimit; }
  
  /**
   * Was the data successfully fetched?
   * @return
   */
  public boolean success() {
    return _success;
  }  
}
