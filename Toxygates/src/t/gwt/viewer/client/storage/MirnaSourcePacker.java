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

package t.gwt.viewer.client.storage;

import javax.annotation.Nullable;

import t.shared.viewer.mirna.MirnaSource;

public class MirnaSourcePacker extends Packer<MirnaSource> {

  @Override
  public String pack(MirnaSource mirnaSource) {
    return mirnaSource.id() + "^^^" + mirnaSource.limit();
  }

  @Nullable
  Double parseNullableDouble(String data) {
    if (data.equals("null")) {
      return null;
    }
    return Double.parseDouble(data);
  }

  @Override
  public MirnaSource unpack(String string) throws UnpackInputException {
    String[] splits = string.split("\\^\\^\\^");
    Double limit;
    
    if (splits.length == 2) {
      try {
        limit = parseNullableDouble(splits[1]);
        return new MirnaSource(splits[0], "", false, limit, 0, null, null, null);
      } catch (NumberFormatException e) {
        throw new UnpackInputException("Could not parse miRNA source limit: " + splits[1]);
      }
    } else {
      throw new UnpackInputException("Malformed serialized miRNA source: expected 2 ^^^-separated " +
          " tokens but found " + splits.length + " in: " + string);
    }
  }
}
