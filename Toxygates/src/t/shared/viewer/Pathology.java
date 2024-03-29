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

package t.shared.viewer;

import java.io.Serializable;
import java.util.Arrays;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class Pathology implements Serializable {
  private String sampleId;
  private String topography;
  private String finding;
  private boolean spontaneous;
  private String grade;
  private String viewerLink;

  public Pathology() {}

  public Pathology(@Nullable String _sampleId, String _topography, String _finding,
      boolean _spontaneous, String _grade, @Nullable String _viewerLink) {
    sampleId = _sampleId;
    topography = _topography;
    finding = _finding;
    spontaneous = _spontaneous;
    grade = _grade;
    viewerLink = _viewerLink;
  }

  public @Nullable String sampleId() {
    return sampleId;
  }

  public String topography() {
    return topography;
  }

  public String finding() {
    return finding;
  }

  public boolean spontaneous() {
    return spontaneous;
  }

  public String grade() {
    return grade;
  }

  @Nullable
  public String viewerLink() {
    return viewerLink;
  }

  @Override
  public int hashCode() {
    int r = 1;
    if (sampleId != null) {
      r = r * 41 + sampleId.hashCode();
    }
    if (topography != null) {
      r = r * 41 + topography.hashCode();
    }
    if (finding != null) {
      r = r * 41 + finding.hashCode();
    }
    if (grade != null) {
      r = r * 41 + grade.hashCode();
    }
    if (spontaneous) {
      r *= 41;
    }
    if (viewerLink != null) {
      r = r * 41 + viewerLink.hashCode();
    }
    return r;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Pathology) {
      Pathology op = (Pathology) other;
      Object[] p1 = new Object[] {sampleId, topography, finding, spontaneous, grade, viewerLink};
      Object[] p2 = new Object[] {op.sampleId(), op.topography(), op.finding(), op.spontaneous(),
          op.grade(), op.viewerLink()};
      return Arrays.deepEquals(p1, p2);
    }
    return false;
  }

}
