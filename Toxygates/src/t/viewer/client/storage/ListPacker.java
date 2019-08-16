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

import java.util.*;
import java.util.stream.Collectors;

import t.common.shared.SharedUtils;

/**
 * Packs/unpacks lists of type T, using a Packer<T>.
 */
public class ListPacker<T> extends Packer<List<T>> {
  private Packer<T> elementPacker;
  private String separator;

  public ListPacker(Packer<T> elementPacker, String separator) {
    this.elementPacker = elementPacker;
    this.separator = separator;
  }

  @Override
  public String pack(List<T> entities) {
    Collection<String> packedEntities = entities.stream()
        .map(e -> elementPacker.pack(e))
        .collect(Collectors.toList());
    return SharedUtils.packList(packedEntities, separator);
  }

  @Override
  public ArrayList<T> unpack(String string) throws UnpackInputException {
    if (string.length() == 0) {
      return new ArrayList<T>(0);
    }
    String[] tokens = string.split(separator);
    // can't use lambdas because elementPacker.unpack throws UnpackInputException
    ArrayList<T> entities = new ArrayList<T>(tokens.length);
    for (String token : tokens) {
      entities.add(elementPacker.unpack(token));
    }
    return entities;
  }
}
