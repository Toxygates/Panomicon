package otgviewer.shared;

import bioweb.shared.Packable;
import bioweb.shared.array.DataColumn;
import bioweb.shared.array.HasSamples;

public interface BarcodeColumn extends Packable, DataColumn<Barcode>, HasSamples<Barcode> {
	/**
	 * Obtain the set of all compounds that the samples in this column are associated with.
	 * @return
	 */
	public String[] getCompounds();
	
	public String pack();
}
