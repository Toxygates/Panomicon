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

package t.viewer.shared.mirna;

import java.io.Serializable;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class MirnaSource implements Serializable {
  //GWT constructor
  MirnaSource() {}
  
  private String title;
  private boolean hasScores, empirical;
  private Double suggestedLimit;
  
  /**
   * Construct a new MiRNA source (mRNA-miRNA associations) information object.
   * 
   * @param title The title of the source
   * @param empirical Whether the source is empirical
   * @param hasScoresWhether the associations have numerical scores
   * @param suggestedLimit The suggested limit, if any (if there are numerical scores)
   */
  public MirnaSource(String title, boolean empirical, 
      boolean hasScores, @Nullable Double suggestedLimit,
      int size) {
    this.title = title;
    this.empirical = empirical;
    this.hasScores = hasScores;
    this.suggestedLimit = suggestedLimit;
  }
  
  public String title() { return title; }
  
  public boolean hasScores() { return hasScores; }
  
  public boolean empirical() { return empirical; }
  
  public @Nullable Double suggestedLimit() { return suggestedLimit; }
}
