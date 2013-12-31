package bioweb.shared.array;
import bioweb.shared.*;


public interface DataColumn<S extends Sample> extends HasSamples<S>, Packable {

	public String getShortTitle();
	
	public String pack();	
}
