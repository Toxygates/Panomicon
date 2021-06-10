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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static t.model.sample.CoreParameter.*;
import static t.model.sample.OTGAttribute.*;

/**
 * An AttributeSet for Open TG-GATEs data.
 */
@SuppressWarnings("serial")
public class OTGAttributeSet extends t.model.sample.AttributeSet {
  
  //GWT constructor
  public OTGAttributeSet() {}
  
  /**
   * Internal constructor. Users should not access this constructor directly.
   */
  OTGAttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    super(attributes, required);
    Collections.addAll(previewDisplay, Dose, DoseUnit, DoseLevel, ExposureTime, AdmRoute, Type);
    Collections.addAll(highLevel, Type, Organism, Organ, TestType, Repeat);
    Collections.addAll(unitLevel, Compound, DoseLevel, ExposureTime);
  }
    
  private static OTGAttributeSet defaultSet;

  /**
   * Obtain the Open TG-GATEs AttributeSet singleton.
   */
  synchronized public static OTGAttributeSet getDefault() {
    if (defaultSet == null) {
      List<Attribute> attributes = new ArrayList<Attribute>();
      Collections.addAll(attributes, CoreParameter.all());
      Collections.addAll(attributes, OTGAttribute.all());
      List<Attribute> required = new ArrayList<Attribute>();
      
      Collections.addAll(required, SampleId, Platform, Type,
              Treatment, ControlTreatment);
      Collections.addAll(required, Organism, TestType, Repeat, Organ,
        Compound, DoseLevel, ExposureTime);
      
      defaultSet = new OTGAttributeSet(attributes, required);
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
