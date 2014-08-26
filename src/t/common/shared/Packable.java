package t.common.shared;

/**
 * A packable object can serialise itself to a string.
 * Deserialisation must be provided elsewhere.
 * @author johan
 *
 */
public interface Packable {
	public String pack();
}
