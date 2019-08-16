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
