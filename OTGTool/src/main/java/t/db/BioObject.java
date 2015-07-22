package t.db;

/**
 * A BioObject is some biological entity that can be uniquely identified
 * by a string. It can also have a name, which by default is the same
 * as the identifier.
 * Equality and hashing should be based on the identifier only.
 */

public interface BioObject {

  String identifier();
  String name();
}
