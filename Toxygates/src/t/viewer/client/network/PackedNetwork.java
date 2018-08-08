package t.viewer.client.network;

import t.viewer.shared.network.Network;

/**
 * A packed network consists of a title String and a JSON String which can be
 * unpacked into (a JavaScript Network which can be converted into) an instance
 * of t.viewer.shared.network.Network.
 */
public class PackedNetwork {
  private String title, jsonString;
  private Network unpacked;

  public PackedNetwork(String title, String jsonString) {
    this.title = title;
    this.jsonString = jsonString;
  }

  public String title() { return title; }
  
  public String jsonString() { return jsonString; }

  public Network unpack() {
    if (unpacked == null) {
      unpacked = NetworkConversion.unpackNetwork(jsonString);
    }
    return unpacked;
  }
}
