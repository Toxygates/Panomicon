package t.viewer.client;

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
    // TODO best location of this? handle viewer/common separation cleanly.
    return SharedUtils.packList(items, separator);
  }
}
