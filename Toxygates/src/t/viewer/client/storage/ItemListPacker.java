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

import t.viewer.shared.clustering.ClusteringList;
import t.common.shared.SharedUtils;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
import t.viewer.shared.clustering.ProbeClustering;

public class ItemListPacker extends Packer<ItemList> {

  @Override
  public String pack(ItemList itemList) {
    return doPack(itemList);
  }

  public static String doPack(ItemList itemList) {
    StringBuilder sb = new StringBuilder();
    sb.append(itemList.type());
    sb.append(":::");
    sb.append(itemList.name());
    sb.append(":::");
    sb.append(SharedUtils.packList(itemList.packedItems(), "^^^"));
    return sb.toString();
  }

  @Override
  public ItemList unpack(String string) throws UnpackInputException {
    return doUnpack(string);
  }
  
  public static ItemList doUnpack(String string) throws UnpackInputException {
    if (string == null) {
      throw new UnpackInputException("Tried to unpack ItemList from null string");
    }

    String[] spl = string.split(":::");
    if (spl.length < 2) {
      throw new UnpackInputException("Tried to unpack ItemList from string with insufficeint fields");
    }

    String type = spl[0];
    String name = spl[1];
    String[] items = spl.length >= 3 ? spl[2].split("\\^\\^\\^") : new String[0];

    // Note: it would be good to avoid having this kind of central registry
    // of list types here. An alternative approach would be that different types
    // register themselves when the corresponding class is loaded/initialized.
    if (type.equals(StringList.PROBES_LIST_TYPE) || type.equals(StringList.COMPOUND_LIST_TYPE)
        || type.equals(ProbeClustering.PROBE_CLUSTERING_TYPE)) {
      return new StringList(type, name, items);
    } else if (type.equals("userclustering")) {
      return new ClusteringList(type, name, items);
    } else {
      throw new UnpackInputException("Tried to unpack ItemList of unknown type");
    }
  }
}
