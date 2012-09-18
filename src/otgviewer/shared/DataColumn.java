package otgviewer.shared;

public interface DataColumn {

	public Barcode[] getBarcodes();
	
	public String[] getCompounds();
	
	public String getShortTitle();
	
	public String pack();
	
}
