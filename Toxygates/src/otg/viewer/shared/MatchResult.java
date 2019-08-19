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

package otg.viewer.shared;

import java.io.Serializable;

/**
 * A result item in a compound ranking.
 * 
 * @author johan
 *
 */
@SuppressWarnings("serial")
public class MatchResult implements Serializable {

  public MatchResult() {}

  private String _fixedValue;

  public String fixedValue() {
    return _fixedValue;
  }

  private double _score;

  public double score() {
    return _score;
  }

  private String _compound;

  public String compound() {
    return _compound;
  }

  public MatchResult(String compound, double score, String fixedValue) {
    _fixedValue = fixedValue;
    _compound = compound;
    _score = score;
  }

  @Override
  public String toString() {
    return "MatchResult(" + _compound + "/" + _fixedValue + " = " + _score + ")";
  }
}
