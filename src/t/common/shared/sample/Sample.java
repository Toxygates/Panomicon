package t.common.shared.sample;
import java.io.Serializable;

import t.common.shared.HasClass;
import t.common.shared.Packable;
import t.common.shared.SampleClass;

/**
 * A microarray sample with a unique identifier that can be represented as a string.
 * @author johan
 * 
 * TODO make non-abstract
 *
 */
abstract public class Sample implements Packable, Serializable, HasClass {

	protected SampleClass sampleClass;
	
	public Sample() {}
	
	public Sample(String _id, SampleClass _sampleClass) {
		id = _id;
		sampleClass = _sampleClass;
	}
	
	private String id;	
	public String id() { return id; }
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	/**
	 * TODO change to a subclass-safe equals with canEqual etc
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Sample) {
			Sample that = (Sample) other;
			return that.canEqual(this) && id.equals(that.id());
		}
		return false;
	}
	
	protected boolean canEqual(Sample other) {
		return other instanceof Sample;
	}
	
	public SampleClass sampleClass() { return sampleClass; }
	
	abstract public String pack();
	
	abstract public Unit<? extends Sample> getUnit();
	
}
