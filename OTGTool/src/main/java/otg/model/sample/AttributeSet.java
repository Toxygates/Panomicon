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

package otg.model.sample;

import static otg.model.sample.OTGAttribute.*;
import static t.model.sample.CoreParameter.*;

import java.util.*;

import t.model.sample.Attribute;
import t.model.sample.CoreParameter;

/**
 * An AttributeSet for Open TG-GATEs data.
 */
@SuppressWarnings("serial")
public class AttributeSet extends t.model.sample.AttributeSet {
  
  //GWT constructor
  public AttributeSet() {}
  
  /**
   * Internal constructor. Users should not access this constructor directly.
   */
  AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    super(attributes, required);
    Collections.addAll(previewDisplay, Dose, DoseUnit, DoseLevel, ExposureTime, AdmRoute, Type);
    Collections.addAll(highLevel, Type, Organism, Organ, TestType, Repeat);
    Collections.addAll(unitLevel, Compound, DoseLevel, ExposureTime);
  }
    
  private static AttributeSet defaultSet;

  /**
   * Obtain the Open TG-GATEs AttributeSet singleton.
   */
  synchronized public static AttributeSet getDefault() {
    if (defaultSet == null) {
      List<Attribute> attributes = new ArrayList<Attribute>();
      Collections.addAll(attributes, CoreParameter.values());
      Collections.addAll(attributes, otg.model.sample.OTGAttribute.values());
      List<Attribute> required = new ArrayList<Attribute>();
      
      Collections.addAll(required, SampleId, ControlGroup, Platform, Type);
      Collections.addAll(required, Organism, TestType, Repeat, Organ,
        Compound, DoseLevel, ExposureTime);
      
      defaultSet = new AttributeSet(attributes, required);
    }
    return defaultSet;
  }
  
  private List<Attribute> previewDisplay = new ArrayList<Attribute>();  
  @Override
  public Collection<Attribute> getPreviewDisplay() { return previewDisplay; }
  
  private List<Attribute> highLevel = new ArrayList<Attribute>();  
  @Override
  public Collection<Attribute> getHighLevel() { return highLevel; }
  
  private List<Attribute> unitLevel = new ArrayList<Attribute>();
  @Override
  public Collection<Attribute> getUnitLevel() { return unitLevel; }

}
