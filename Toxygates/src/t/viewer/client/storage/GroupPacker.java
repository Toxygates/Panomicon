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

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.common.shared.sample.*;

public class GroupPacker extends Packer<Group> {
  private SamplePacker samplePacker;
  private DataSchema schema;

  public GroupPacker(SamplePacker samplePacker, DataSchema schema) {
    this.samplePacker = samplePacker;
    this.schema = schema;
  }

  @Override
  public String pack(Group group) {
    StringBuilder s = new StringBuilder();
    s.append("Group:::");
    s.append(group.getName() + ":::"); // !!
    s.append(group.getColor() + ":::");
    for (Sample sample : group.samples()) {
      s.append(samplePacker.pack(sample));
      s.append("^^^");
    }
    return s.toString();
  }

  @Override
  public Group unpack(String string) throws UnpackInputException {
    if (string == null) {
      return null;
    }

    // This check for a legacy format is probably not necessary now that we have 
    // better exceptions downstream
    String[] spl = string.split("\\$\\$\\$");
    if (spl[0].equals("Barcode") || spl[0].equals("Barcode_v3")) {
      throw new UnpackInputException("Unsupported legacy column format: " + string);
    }

    String[] s1 = string.split(":::"); // !!
    if (s1.length != 4) {
      throw new UnpackInputException("Malformed serialized group: wrong number of "
          + ":::-separated tokens. Should be 4, but found " + s1.length + ".");
    }
    String name = s1[1];
    String color = "";
    String sampleIds = "";

    color = s1[2];
    sampleIds = s1[3];
    if (SharedUtils.indexOf(SampleGroup.groupColors, color) == -1) {
      // replace the color if it is invalid.
      // this lets us safely upgrade colors in the future.
      color = SampleGroup.groupColors[0];
    }

    String[] s2 = sampleIds.split("\\^\\^\\^");
    Sample[] bcs = new Sample[s2.length];
    for (int i = 0; i < s2.length; ++i) {
      bcs[i] = samplePacker.unpack(s2[i]);
    }
    // DataFilter useFilter = (bcs[0].getUnit().getOrgan() == null) ? filter : null;
    return new Group(schema, name, bcs, color);
  }
}
