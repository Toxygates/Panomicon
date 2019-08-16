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

package t.viewer.client.storage;

import java.util.HashMap;
import java.util.Map;

import t.model.SampleClass;
import t.model.sample.*;

public class SampleClassPacker extends Packer<SampleClass> {
  AttributeSet attributes;

  public SampleClassPacker(AttributeSet attributes) {
    this.attributes = attributes;
  }

  @Override
  public String pack(SampleClass sampleClass) {
    StringBuilder sb = new StringBuilder();
    for (Attribute attribute : sampleClass.getKeys()) {
      sb.append(attribute.id() + ",,,");
      sb.append(sampleClass.get(attribute) + ",,,");
    }
    return sb.toString();
  }

  @Override
  public SampleClass unpack(String string) {
    String[] splits = string.split(",,,");
    Map<Attribute, String> d = new HashMap<Attribute, String>();
    for (int i = 0; i < splits.length - 1; i += 2) {
      Attribute attribute = attributes.byId(splits[i]);
      if (attribute != null) {
        d.put(attribute, splits[i + 1]);
      }
    }

    if (!d.containsKey(CoreParameter.Type)) {
      upgradeSampleClass(d);
    }

    return new SampleClass(d);
  }

  //Transitional method for upgrading from old format, as of Jan 2018
  private static void upgradeSampleClass(Map<Attribute, String> data) {
    data.put(CoreParameter.Type, "mRNA");
  }
}
