package bioweb.shared.array;

import java.io.Serializable;

import otgviewer.shared.Barcode;

/**
 * A microarray sample.
 * @author johan
 *
 */
public abstract class Sample implements Serializable {

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
	
}
