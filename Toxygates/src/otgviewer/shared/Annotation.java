/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otgviewer.shared;

import java.io.Serializable;
import java.util.List;

/**
 * A set of annotations (such as chemical data and morphological data) corresponding to a
 * sample.
 */
@SuppressWarnings("serial")
public class Annotation implements Serializable {
  
  /**
   * @param id The sample ID.
   * @param annotations
   */
  public Annotation(String id, List<BioParamValue> values) {
    _id = id;
    _values = values;
  }

  public Annotation() {}

  private String _id;

  public String id() {
    return _id;
  }

  private List<BioParamValue> _values;

  public List<BioParamValue> getAnnotations() {
    return _values;
  }
}
