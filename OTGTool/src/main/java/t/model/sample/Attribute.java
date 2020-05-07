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

package t.model.sample;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * An attribute of a sample
 */
public class Attribute implements Serializable {

  private String id, title;
  private @Nullable String section;
  private boolean isNumerical;

  //GWT constructor
  public Attribute() {}

  public Attribute(String id, String title, boolean isNumerical, @Nullable String section) {
    this.id = id;
    this.title = title;
    this.isNumerical = isNumerical;
    this.section = section;
  }

  public Attribute(String id, String title, String kind, @Nullable String section) {
    this(id, title, "numerical".equals(kind), section);
  }

  public Attribute(String id, String title) {
    this(id, title, false, null);
  }

  /**
   * Internal ID for database purposes
   */
  public String id() { return id; }
  
  /**
   * Human-readable title
   */
  public String title() { return title; }
  
  /**
   * Whether the attribute is numerical
   */
  public boolean isNumerical() { return isNumerical; }
  
  /**
   * The section that the attribute belongs to, if any.
   */
  public @Nullable String section() { return section; }

  @Override
  public String toString() {
    return title;
  }

  @Override
  public int hashCode() {
    return AttributeSet.attributeHash(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Attribute) {
      return AttributeSet.attributesEqual(this, (Attribute) obj);
    } else {
      return false;
    }
  }

  public static final String NOT_AVAILABLE = "na";

  /**
   * Marker for values that were measured, but the result is judged to be not defined.
   */
  public static final String UNDEFINED_VALUE = "undef";

  /**
   * Marker for values that were measured, but the result is judged to be not defined.
   */
  public static final String UNDEFINED_VALUE_2 = "n.d";
}
