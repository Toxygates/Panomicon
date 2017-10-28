/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SharedUtils {
  public static <T> int indexOf(T[] haystack, T needle) {
    for (int i = 0; i < haystack.length; ++i) {
      if (haystack[i].equals(needle)) {
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
    Stream<String> ss = cl.stream().
        map(x -> (x == null ? "null" : x.toString())).sorted();
    
    return ss.collect(Collectors.joining (separator));    
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
    items.stream().forEachOrdered(i -> sb.append(i + separator));    
    return sb.toString();
  }
}
