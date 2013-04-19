package bioweb.shared.array;
import bioweb.shared.*;

public interface DataColumn<S extends Sample> extends Packable {

	public S[] getBarcodes();
	
	public String getShortTitle();
	
	public String pack();	
}
