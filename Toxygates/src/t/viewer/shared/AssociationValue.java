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

import java.io.Serializable;

import javax.annotation.Nullable;

import t.common.shared.Pair;

@SuppressWarnings("serial")
public class AssociationValue implements Serializable {
  private String title;
  private String formalIdentifier;
  private @Nullable String tooltip;
  
  //GWT constructor
  AssociationValue() {}
  
  public AssociationValue(String title, String formalIdentifier, @Nullable String tooltip) {
    this.title = title;
    this.formalIdentifier = formalIdentifier;
    this.tooltip = tooltip;
  }
  
  public AssociationValue(Pair<String, String> value) {
    this(value.first(), value.second(), null);
  }
  
  public String title() {
    return title;
  }
  
  public String formalIdentifier() {
    return formalIdentifier;    
  }
  
  @Override
  public int hashCode() {
    return title.hashCode() * 41 + formalIdentifier.hashCode(); 
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof AssociationValue) {
      return formalIdentifier.equals(((AssociationValue) other).formalIdentifier());
    } else {
      return false;
    }
  }
  
  public String tooltip() {
    if (tooltip == null) {
      if (!formalIdentifier.equals(title)) {
        return formalIdentifier +": " + title;
      } else {
        return title;
      }       
    } else {
      return tooltip;
    }
  }  
}
