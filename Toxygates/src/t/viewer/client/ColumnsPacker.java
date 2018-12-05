package t.viewer.client;


import java.util.*;

import javax.annotation.Nullable;

import t.common.shared.sample.Group;

public class ColumnsPacker extends Packer<Collection<Group>> {
  private GroupPacker groupPacker;

  public ColumnsPacker(GroupPacker groupPacker) {
    this.groupPacker = groupPacker;
  }

  @Override
  @Nullable
  // Separator hierarchy for columns:
  // ### > ::: > ^^^ > $$$
  public List<Group> unpack(String string) throws UnpackInputException {
    List<Group> r = new ArrayList<Group>();
    if (string != null) {
      String[] spl = string.split("###");
      for (String cl : spl) {
        Group c = groupPacker.unpack(cl);
        r.add(c);
      }
    }
    return r;
  }

  @Override
  public String pack(Collection<Group> columns) {
    List<String> xs = new ArrayList<String>();
    for (Group group : columns) {
      xs.add(groupPacker.pack(group));
    }
    return packList(xs, "###");
  }
}
