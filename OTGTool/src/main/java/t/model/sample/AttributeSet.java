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

import java.io.Serializable;
import java.util.*;
import javax.annotation.Nullable;
import static t.model.sample.CoreParameter.*;

/**
 * A mutable set of sample attributes. The findOrCreate operation can be used
 * to look up or add attributes.
 */
@SuppressWarnings("serial")
public class AttributeSet implements Serializable {
 
  //GWT constructor
  public AttributeSet() {}
  
  /**
   * Construct a new attribute set.*
   * @param attributes All attributes in the set.
   * @param required The subset of attributes that are required to be present in new batches.
   */
  public AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    this.required = required;
    for (Attribute a: attributes) {
      add(a);
    }
  }

  /**
   * Obtain a new minimal attribute set containing all core attributes. This can be used as a template
   * for building larger sets.
   */
  public static AttributeSet newMinimalSet() {
    List<Attribute> attributes = new ArrayList<Attribute>();
    Collections.addAll(attributes, SampleId, Treatment, ControlTreatment, Batch, Platform, Type);
    List<Attribute> required = new ArrayList<Attribute>();
    Collections.addAll(required, SampleId, Treatment, ControlTreatment, Batch, Platform, Type);
    return new AttributeSet(attributes, required);
  }
  
  protected Collection<Attribute> attributes = new ArrayList<Attribute>();
  protected Collection<Attribute> required;
  
  protected Map<String, Attribute> byId = new HashMap<String, Attribute>();  
  protected Map<String, Attribute> byTitle = new HashMap<String, Attribute>();
  
  public Collection<Attribute> getAll() {
    return attributes;
  }
  
  public List<Attribute> getAllByKind(boolean isNumerical) {
    List<Attribute> r = new ArrayList<Attribute>();
    for (Attribute a: attributes) {
      if (a.isNumerical() == isNumerical) {
        r.add(a);
      }
    }
    return r;
  }
  
  public List<Attribute> getNumerical() {
    return getAllByKind(true);
  }
  
  public List<Attribute> getString() {
    return getAllByKind(false);
  }
  
  /**
   * Get all attributes that are required to be present in new batches.
   */
  public Collection<Attribute> getRequired() {
    return required;
  }

  /**
   * Get all attributes that are suitable for a preview display (a brief overview of a 
   * set of samples).
   */
  public Collection<Attribute> getPreviewDisplay() {
    return required;
  }
  
  /**
   * Get all attributes that are suitable for a high level grouping of samples.
   */
  public Collection<Attribute> getHighLevel() { return Collections.emptyList(); }
  
  /**
   * Get all attributes that are sufficient for distinguishing units within the high-level grouping
   */
  public Collection<Attribute> getUnitLevel() { return Collections.emptyList(); }
  
  public @Nullable Attribute byId(String id) {
    return byId.get(id);
  }
  
  public @Nullable Attribute byTitle(String title) {
    return byTitle.get(title);
  }
  
  private void add(Attribute a) {
    attributes.add(a);
    byId.put(a.id(), a);
    byTitle.put(a.title(), a);
  }
  
  /**
   * Find the attribute with the given id or create it (and add it to this set) if it
   * doesn't exist.
   */
  public Attribute findOrCreate(String id, @Nullable String title,
                                @Nullable String kind) {
    return findOrCreate(id, title, kind, null);
  }
  
  
  /**
   * Find the attribute with the given id or create it (and add it to this set) if it
   * doesn't exist.
   */
  synchronized public Attribute findOrCreate(String id, @Nullable String title,
      @Nullable String kind, @Nullable String section) {
    if (byId.containsKey(id)) {
      return byId.get(id);
    }
    
    Attribute a = new Attribute(id, title, kind, section);
    add(a);
    return a;
  }
  
  public static int compare(Attribute a1, Attribute a2) {
    return a1.title().compareTo(a2.title());
  }
  
  public static boolean attributesEqual(Attribute a1, Attribute a2) {
    return a1.id().equals(a2.id());
  }
  
  public static int attributeHash(Attribute a1) {
    return a1.id().hashCode();
  }
}
