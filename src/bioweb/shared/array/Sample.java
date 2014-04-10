package bioweb.shared.array;
import bioweb.shared.*;

import java.io.Serializable;

/**
 * A microarray sample with a unique identifier that can be represented as a string.
 * @author johan
 *
 */
public abstract class Sample implements Packable, Serializable {

	public Sample() {}
	
	public Sample(String id) {
		_id = id;
	}
	
	private String _id;	
	public String id() { return _id; }
	
	@Override
	public int hashCode() {
		return _id.hashCode();
	}
	
	/**
	 * TODO change to a subclass-safe equals with canEqual etc
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Sample) {
			Sample that = (Sample) other;
			return that.canEqual(this) && _id.equals(that.id());
		}
		return false;
	}
	
	protected boolean canEqual(Sample other) {
		return other instanceof Sample;
	}
	
	abstract public String pack();
	
	abstract public Unit<? extends Sample> getUnit();
	
}
