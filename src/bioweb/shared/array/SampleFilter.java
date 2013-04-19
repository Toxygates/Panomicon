package bioweb.shared.array;
import bioweb.shared.*;
import java.io.Serializable;

/**
 * A way of filtering microarray samples (work in progress).
 * @author johan
 *
 * @param <S>
 */
abstract public class SampleFilter<S extends Sample> implements Packable, Serializable {

	public SampleFilter() {}
	
	abstract public String pack();
}
