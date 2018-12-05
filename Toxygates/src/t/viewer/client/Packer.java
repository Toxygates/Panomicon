package t.viewer.client;

import java.util.*;
import java.util.logging.Logger;

import t.common.shared.*;
import t.viewer.shared.ItemList;

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

  public static String packDatasets(Dataset[] datasets) {
    List<String> r = new ArrayList<String>();
    for (Dataset d : datasets) {
      r.add(d.getId());
    }
    return packList(r, "###");
  }

  public String packCompounds(List<String> compounds) {
    return packList(compounds, "###");
  }
}
