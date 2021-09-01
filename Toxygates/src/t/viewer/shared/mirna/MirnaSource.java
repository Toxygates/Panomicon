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

package t.viewer.shared.mirna;

import java.io.Serializable;
import java.util.*;

import javax.annotation.Nullable;

import t.shared.common.Pair;

@SuppressWarnings("serial")
public class MirnaSource implements Serializable  {
  //GWT constructor
  MirnaSource() {}
  
  private String title, id;
  private boolean hasScores;
  private @Nullable Double limit;
  private int size;
  private @Nullable String comment;
  private @Nullable List<? extends Pair<String, Double>> scoreLevels;
  private @Nullable String infoURL;
  
  /**
   * Construct a new MiRNA source (mRNA-miRNA associations) information object.
   * 
   * @param id An identifying string. 
   * @param title The title of the source
   * @param experimental Whether the source is experimentally validated
   * @param hasScoresWhether the associations have numerical scores
   * @param limit The cutoff limit, if any (if there are numerical scores)
   * @param comment
   * @param scoreLevels If given, the user will be asked to select from these. 
   * labelled score levels from a drop-down box. The sort order will be preserved.
   */
  public MirnaSource(String id, String title,  
      boolean hasScores, @Nullable Double limit,
      int size, @Nullable String comment,
      @Nullable List<? extends Pair<String, Double>> scoreLevels,
      @Nullable String infoURL) {
    this.id = id;
    this.title = title;    
    this.hasScores = hasScores;
    this.limit = limit;
    this.size = size;
    this.comment = comment;
    this.scoreLevels = scoreLevels;
    this.infoURL = infoURL;
  }
  
  public String title() { return title; }
  
  public boolean hasScores() { return hasScores; }
  
  public @Nullable Double limit() { return limit; }
  
  public void setLimit(Double limit) { this.limit = limit; }
  
  public String id() { return id; }
  
  public int size() { return size; }
  
  public @Nullable String comment() { return comment; }
  
  public @Nullable List<? extends Pair<String, Double>> scoreLevels() { return scoreLevels; }
  
  public @Nullable String infoURL() { return infoURL; }
  
  public @Nullable Map<String, Double> scoreLevelMap() {
    if (scoreLevels == null) {
      return null;
    }
    Map<String, Double> r = new HashMap<String, Double>();
    for (Pair<String,Double> p : scoreLevels) {
      r.put(p.first(), p.second());
    }
    return r;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof MirnaSource) {
      return id.equals(((MirnaSource) other).id());
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }  
}
