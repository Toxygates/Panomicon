package otgviewer.shared;

import bioweb.shared.array.DataColumn;

public interface BarcodeColumn extends DataColumn<Barcode> {
	/**
	 * Obtain the set of all compounds that the samples in this column are associated with.
	 * @return
	 */
	public String[] getCompounds();
}
