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

import javax.annotation.Nullable;

import t.shared.common.sample.Sample;
import t.model.SampleClass;

public class SamplePacker extends Packer<Sample> {
  private SampleClassPacker sampleClassPacker;

  public SamplePacker(SampleClassPacker sampleClassPacker) {
    this.sampleClassPacker = sampleClassPacker;
  }

  @Override
  public String pack(Sample sample) {
    final String sep = "$$$";
    StringBuilder sb = new StringBuilder();
    sb.append("Barcode_v3").append(sep);
    sb.append(sample.id()).append(sep);
    sb.append(sampleClassPacker.pack(sample.sampleClass())).append(sep);
    return sb.toString();
  }

  @Override
  public @Nullable Sample unpack(String string) throws UnpackInputException {
    String[] spl = string.split("\\$\\$\\$");
    if (spl.length != 3) {
      throw new UnpackInputException("Malformed serialized sample: wrong number of "
          + "$$$-separated tokens. Should be 3, but found " + spl.length + ".");
    }
    String v = spl[0];
    if (!v.equals("Barcode_v3")) {
      throw new UnpackInputException("Legacy data has been detected in your browser's storage. "
          + "Some of your older sample groups may not load properly.");
    }
    String id = spl[1];
    SampleClass sc = sampleClassPacker.unpack(spl[2]);
    return new Sample(id, sc);
  }
}
