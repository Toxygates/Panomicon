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

package t.common.shared;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public abstract class ManagedItem implements Serializable, DataRecord {

  protected String title, comment;
  protected Date date;

  public ManagedItem() {}

  public ManagedItem(String title, String comment, Date date) {
    this.title = title;
    this.comment = comment;
    this.date = date;
  }

  // TODO rename
  public String getTitle() {
    return title;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String c) {
    comment = c;
  }

  public Date getDate() {
    return date;
  }

  // TODO rename
  public String getUserTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getClass().toString() + ":" + title;
  }

  @Override
  public int hashCode() {
    return title.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ManagedItem) {
      ManagedItem mi = (ManagedItem) other;
      return mi.getTitle().equals(title);
    }
    return false;
  }
}
