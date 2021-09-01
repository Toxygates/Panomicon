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

package t.shared.common;

import java.util.Date;

@SuppressWarnings("serial")
public class Platform extends ManagedItem {

  private int numProbes;
  private String publicComment;

  public Platform() {}

  public Platform(String id, int numProbes, String comment, Date date, String publicComment) {
    super(id, comment, date);
    this.numProbes = numProbes;
    this.publicComment = publicComment;
  }

  public int getNumProbes() {
    return numProbes;
  }

  public String getPublicComment() {
    return publicComment;
  }
}