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

import t.common.shared.AType;

public interface SortKey extends Serializable {
  @SuppressWarnings("serial")
  class MatrixColumn implements SortKey {
    public int matrixIndex;

    public MatrixColumn() {}

    public MatrixColumn(int idx) {
      this.matrixIndex = idx;
    }
  }

  @SuppressWarnings("serial")
  class Association implements SortKey {
    public AType atype;

    public Association() {}

    public Association(AType assoc) {
      this.atype = assoc;
    }
  }
}
