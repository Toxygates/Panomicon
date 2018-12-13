package t.viewer.client.storage;

import java.util.function.Consumer;
import java.util.function.Supplier;

import t.viewer.client.storage.Packer.UnpackInputException;

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

  public void store(T value) {
    if (value == null) {
      storageProvider.clearItem(key);
    } else {
      String string = packer.pack(value);
      storageProvider.setItem(key, string);
    }
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
