package bioweb.shared.array;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A group of samples produced under identical experimental conditions,
 * i.e. all samples corresponding to a particular combination of experimental
 * parameters.
 */
public class Unit<S extends Sample> implements HasSamples<S>, Serializable {
	protected String _name;
	private S[] _samples = null;
	
	protected Unit() {}
	
	public Unit(String name) {
		_name = name;
	}
	
	public String toString() {
		return _name;
	}
	
	public void setSamples(S[] samples) {
		_samples = samples;
	}
	
	/**
	 * The samples in this group (if they have been set)
	 */
	@Nullable
	public S[] getSamples() {
		return _samples;
	}

}
