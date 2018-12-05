package t.viewer.client;

import java.util.Collection;
import java.util.logging.Logger;

import t.common.shared.SharedUtils;

/**
 * Methods for packing objects into strings, and unpacking from strings,
 * factored out of StorageParser.
 */
abstract public class Packer<T> {
  protected static final Logger logger = SharedUtils.getLogger("storage");

  abstract public String pack(T entity);
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
