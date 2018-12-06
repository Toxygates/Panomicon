package t.viewer.client;

import t.viewer.client.network.PackedNetwork;

public class PackedNetworkPacker extends Packer<PackedNetwork> {

  @Override
  public String pack(PackedNetwork network) {
    return network.title() + ":::" + network.jsonString();
  }

  @Override
  public PackedNetwork unpack(String string) throws UnpackInputException {
    String[] splits = string.split(":::");
    if (splits.length == 2) {
      return new PackedNetwork(splits[0], splits[1]);
    } else {
      throw new UnpackInputException("Malformed serialized PackedNetwork: wrong number of :::-separated tokens. "
          + "Should be 2; found " + splits.length + ".");
    }
  }
}
