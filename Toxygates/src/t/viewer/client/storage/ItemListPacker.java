package t.viewer.client.storage;

import t.clustering.shared.ClusteringList;
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
  public ItemList unpack(String string) {
    return doUnpack(string);
  }

  public static ItemList doUnpack(String string) {
    if (string == null) {
      return null;
    }

    String[] spl = string.split(":::");
    if (spl.length < 3) {
      return null;
    }

    String type = spl[0];
    String name = spl[1];
    String[] items = spl[2].split("\\^\\^\\^");

    // Note: it would be good to avoid having this kind of central registry
    // of list types here. An alternative approach would be that different types
    // register themselves when the corresponding class is loaded/initialized.
    if (type.equals(StringList.PROBES_LIST_TYPE) || type.equals(StringList.COMPOUND_LIST_TYPE)
        || type.equals(ProbeClustering.PROBE_CLUSTERING_TYPE)) {
      return new StringList(type, name, items);
    } else if (type.equals("userclustering")) {
      return new ClusteringList(type, name, items);
    } else {
      // Unexpected type, ignore
      return null;
    }
  }
}
