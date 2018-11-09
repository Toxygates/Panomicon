package t.viewer.client.network;

import com.google.gwt.core.client.JavaScriptObject;

import t.viewer.shared.network.Network;

/**
 * A packed network consists of a title String and a JSON String which can be
 * unpacked into (a JavaScript Network which can be converted into) an instance
 * of t.viewer.shared.network.Network.
 */
public class PackedNetwork {
  private String title, jsonString;
  private Network unpacked;
  private JavaScriptObject unpackedJS;

  public PackedNetwork(String title, String jsonString) {
    this.title = title;
    this.jsonString = jsonString;
  }

  public String title() { return title; }
  
  public String jsonString() { return jsonString; }

  public Network unpack() {
    unpackJS();
    if (unpacked == null) {
      unpacked = NetworkConversion.convertNetworkToJava(unpackedJS);
    }
    return unpacked;
  }
  
  public JavaScriptObject unpackJS() {
    if (unpackedJS == null) {
      unpackedJS = NetworkConversion.unpackToJavaScript(jsonString);
    }
    return unpackedJS;
  }
}
