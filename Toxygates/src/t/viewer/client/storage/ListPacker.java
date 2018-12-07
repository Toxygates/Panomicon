package t.viewer.client.storage;

import java.util.*;
import java.util.stream.Collectors;

import t.common.shared.SharedUtils;

/**
 * Packs/unpacks lists of type T, using a Packer<T>.
 */
public class ListPacker<T> extends Packer<List<T>> {
  private Packer<T> elementPacker;
  private String separator;

  public ListPacker(Packer<T> elementPacker, String separator) {
    this.elementPacker = elementPacker;
    this.separator = separator;
  }

  @Override
  public String pack(List<T> entities) {
    Collection<String> packedEntities = entities.stream()
        .map(e -> elementPacker.pack(e))
        .collect(Collectors.toList());
    return SharedUtils.packList(packedEntities, separator);
  }

  @Override
  public ArrayList<T> unpack(String string) throws UnpackInputException {
    if (string.length() == 0) {
      return new ArrayList<T>(0);
    }
    String[] tokens = string.split(separator);
    // can't use lambdas because elementPacker.unpack throws UnpackInputException
    ArrayList<T> entities = new ArrayList<T>(tokens.length);
    for (String token : tokens) {
      entities.add(elementPacker.unpack(token));
    }
    return entities;
  }
}
