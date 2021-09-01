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

package t.gwt.viewer.client.storage;

import java.util.function.Consumer;
import java.util.function.Supplier;

import t.gwt.viewer.client.storage.Packer.UnpackInputException;

/**
 * Stores objects into browser local storage using GWT's Storage class, and 
 * also retrieves them. Since local storage can only store strings, we use a
 * Packer<T> to convert objects to and from strings so they can be stored. 
 */
public class Storage<T> {
  private String key;
  private Packer<T> packer;
  private StorageProvider storageProvider;
  
  /**
   * Supplies values to be returned by get() when storageProvider.getItem()
   * returns null, or by getIgnoringException when an UnpackInputException is
   * thrown.
   */
  private Supplier<T> defaultValueProvider = () -> null;

  public interface StorageProvider {
    void setItem(String key, String value);
    String getItem(String key);
    void clearItem(String key);
  }

  public Storage(String key, Packer<T> packer, StorageProvider storageProvider) {
    this.key = key;
    this.packer = packer;
    this.storageProvider = storageProvider;
  }

  public Storage(String key, Packer<T> packer, StorageProvider storageProvider, Supplier<T> defaultValueProvider) {
    this(key, packer, storageProvider);
    this.defaultValueProvider = defaultValueProvider;
  }

  public T get() throws UnpackInputException {
    String string = storageProvider.getItem(key);
    if (string == null) {
      return defaultValueProvider.get();
    }
    T value = packer.unpack(string);
    return value;
  }

  public T store(T value) {
    if (value == null) {
      storageProvider.clearItem(key);
    } else {
      String string = packer.pack(value);
      storageProvider.setItem(key, string);
    }
    return value;
  }

  /**
   * Like get(), except that if an UnpackInputException is thrown, it is consumed
   * by exceptionHandler, and a value from defaultValueProvider is returned. Callers
   * should be sure that they can smoothly handle the default value. 
   */
  public T getWithExceptionHandler(Consumer<UnpackInputException> exceptionHandler) {
    try {
      return get();
    } catch (UnpackInputException e) {
      exceptionHandler.accept(e);
      return defaultValueProvider.get();
    }
  }

  private final Consumer<UnpackInputException> exceptionIgnorer = e -> {};

  /**
   * Like getWithExceptionHandler, except that exceptions are silently ignored. Should
   * only be used when silently losing malformed or outdated data is acceptable. Callers
   * should also be sure that they can smoothly handle the default value.
   */
  public T getIgnoringException() {
    return getWithExceptionHandler(exceptionIgnorer);
  }
}
