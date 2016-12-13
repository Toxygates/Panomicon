/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.i18n.client.NumberFormat;

public class SharedUtils {
  public static <T> int indexOf(T[] haystack, T needle) {
    for (int i = 0; i < haystack.length; ++i) {
      if (haystack[i].equals(needle)) {
        return i;
      }
    }
    return -1;
  }

  public static <T> int indexOf(List<T> haystack, T needle) {
    for (int i = 0; i < haystack.size(); ++i) {
      if (haystack.get(i).equals(needle)) {
        return i;
      }
    }
    return -1;
  }

  public static String mkString(String[] ar) {
    return mkString(ar, "");
  }

  public static String mkString(String[] ar, String separator) {
    return mkString(Arrays.asList(ar), separator);
  }

  public static String mkString(String beforeEach, String[] ar, String afterEach) {
    return beforeEach + mkString(ar, afterEach + beforeEach) + afterEach;
  }

  public static String mkString(Collection<? extends Object> cl, String separator) {
    List<String> ss = new ArrayList<String>();
    for (Object o : cl) {
      if (o == null) {
        ss.add("null");
      } else {
        ss.add(o.toString());
      }
    }
    java.util.Collections.sort(ss);
    StringBuilder sb = new StringBuilder();
    for (String s : ss) {
      sb.append(s);
      sb.append(separator);
    }
    String r = sb.toString();
    if (r.length() > 0) {
      return r.substring(0, r.length() - separator.length()); // remove final separator
    } else {
      return r;
    }
  }

  public static Logger getLogger() {
    return getLogger("default");
  }

  public static Logger getLogger(String suffix) {
    return Logger.getLogger("jp.level-five.tframework." + suffix);
  }

  // TODO best location for this?
  public static String packList(Collection<String> items, String separator) {
    StringBuilder sb = new StringBuilder();
    for (String x : items) {
      sb.append(x);
      sb.append(separator);
    }
    return sb.toString();
  }
  
  private static NumberFormat df = NumberFormat.getDecimalFormat();
  private static NumberFormat sf = NumberFormat.getScientificFormat();

  public static String formatNumber(double v) {
    if (v == 0.0) {
      return "0";
    }
    if (Math.abs(v) > 0.001) {
      return df.format(v);
    } else {
      return sf.format(v);
    }
  }
}
