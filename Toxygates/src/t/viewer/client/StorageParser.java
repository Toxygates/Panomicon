/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;

import t.common.client.Utils;
import t.common.shared.*;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.model.SampleClass;
import t.model.sample.AttributeSet;

/**
 * Storage parsing/serialising code. Some is still spread out in other classes, 
 * such as Group.
 * PersistedState may also be considered for some of these items in the future, where 
 * lifecycle management is needed.
 */
public class StorageParser {

  private final String prefix;
  private final Storage storage;
  private static final char[] reservedChars = new char[] {':', '#', '$', '^'};
  public static final String unacceptableStringMessage =
      "The characters ':', '#', '$' and '^' are reserved and may not be used.";

  protected static final Logger logger = SharedUtils.getLogger("storage");

  public StorageParser(Storage storage, String prefix) {
    this.prefix = prefix;
    this.storage = storage;
  }

  public void setItem(String key, String value) {
    storage.setItem(prefix + "." + key, value);
    // logger.info("SET " + prefix + "." + key + " -> " + value);
  }

  public String getItem(String key) {
    String v = storage.getItem(prefix + "." + key);
    // logger.info("GET " + prefix + "." + key + " -> " + v);
    return v;
  }

  public void clearItem(String key) {
    storage.removeItem(prefix + "." + key);
  }

  public static String packColumns(Collection<? extends SampleColumn> columns) {
    return packPackableList(columns, "###");
  }

  @Nullable
  public static Group unpackColumn(DataSchema schema, String s, AttributeSet attributes) {
    if (s == null) {
      return null;
    }
    String[] spl = s.split("\\$\\$\\$");
    if (!spl[0].equals("Barcode") && !spl[0].equals("Barcode_v3")) {
      return Group.unpack(schema, s, attributes);
    } else {
      // Legacy or incorrect format
      logger.warning("Unexpected column format: " + s);
      return null;
    }
  }

  public static String packProbes(String[] probes) {
    return packList(Arrays.asList(probes), "###");
  }

  public static String packPackableList(Collection<? extends Packable> items, String separator) {
    List<String> xs = new ArrayList<String>();
    for (Packable p : items) {
      xs.add(p.pack());
    }
    return packList(xs, separator);
  }

  public static String packList(Collection<String> items, String separator) {
    // TODO best location of this? handle viewer/common separation cleanly.
    return SharedUtils.packList(items, separator);
  }

  public static String packItemLists(Collection<ItemList> lists, String separator) {
    return packPackableList(lists, separator);
  }

  public static boolean isAcceptableString(String test, String failMessage) {
    for (char c : reservedChars) {
      if (test.indexOf(c) != -1) {
        Window.alert(failMessage + " " + unacceptableStringMessage);
        return false;
      }
    }
    return true;
  }

  @Nullable
  public SampleClass getSampleClass(AttributeSet attributes) {
    String v = getItem("sampleClass");
    if (v == null) {
      return null;
    } else {
      return Utils.unpackSampleClass(attributes, v);
    }
  }

  @Nullable
  // Separator hierarchy for columns:
  // ### > ::: > ^^^ > $$$
  public List<Group> getColumns(DataSchema schema, String key, Collection<? extends SampleColumn> expectedColumns,
      AttributeSet attributes) throws Exception {
    // TODO unpack old format columns
    String v = getItem(key);
    List<Group> r = new ArrayList<Group>();
    if (v == null) {
      return null;
    }
    String[] spl = v.split("###");
    for (String cl : spl) {
      Group c = unpackColumn(schema, cl, attributes);
      r.add(c);
    }
    return r;
  }
}
