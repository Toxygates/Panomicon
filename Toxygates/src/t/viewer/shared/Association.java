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

import java.io.Serializable;
import java.util.*;

import t.common.shared.AType;

/**
 * An association is a mapping from probes to other objects. They are used as "dynamic columns" in
 * the GUI. The mapped-to objects have names and formal identifiers.
 */
@SuppressWarnings("serial")
public class Association implements Serializable {

  private AType _type;
  private Map<String, ? extends Set<AssociationValue>> _data =
      new HashMap<String, HashSet<AssociationValue>>();
  private boolean _overSizeLimit;
  private boolean _success;

  public Association() {}

  /**
   * Construct a successful association result.
   * @param type
   * @param data Association data keyed on probe id:s.
   */
  public Association(AType type, Map<String, ? extends Set<AssociationValue>> data,
      boolean overSizeLimit, boolean success) {
    this(type, success);
    _data = data;
    _overSizeLimit = overSizeLimit;
  }
  
  /**
   * Construct an empty, possibly failed Association result.
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
  
  public boolean overSizeLimit() {
    return _overSizeLimit;
  }
  
  /**
   * Was the data successfully fetched?
   * @return
   */
  public boolean success() {
    return _success;
  }  
}
