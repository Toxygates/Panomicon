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

import java.util.Collection;
import java.util.logging.Logger;

import t.common.shared.SharedUtils;

/**
 * Converts (packs) objects of a given type into a string, and also converts
 * (unpacks) such strings back into an object of that type.
 */
abstract public class Packer<T> {
  protected static final Logger logger = SharedUtils.getLogger("storage");

  abstract public String pack(T entity);

  /**
   * Unpacks a string into an object of type T. Not expected to handle cases where
   * the input is null.
   */
  abstract public T unpack(String string) throws UnpackInputException;

  /**
   * Thrown when unpacking an object from a string fails because of bad input
   * data.
   */
  @SuppressWarnings("serial")
  public static class UnpackInputException extends Exception {
    public UnpackInputException(String message) {
      super(message);
    }
  }

  public static String packList(Collection<String> items, String separator) {
    return SharedUtils.packList(items, separator);
  }
}
