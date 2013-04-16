package bioweb.shared.array;

public interface DataColumn<S extends Sample> {

	public S[] getBarcodes();
	
	public String getShortTitle();
	
	public String pack();	
}
