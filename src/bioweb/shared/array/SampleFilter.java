package bioweb.shared.array;

import java.io.Serializable;

abstract public class SampleFilter<S extends Sample> implements Serializable {

	public SampleFilter() {}
	
	abstract public String pack();
}
