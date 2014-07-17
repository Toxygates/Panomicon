package otgviewer.shared;

import t.common.shared.Packable;
import t.common.shared.sample.DataColumn;
import t.common.shared.sample.HasSamples;

public interface BarcodeColumn extends Packable, DataColumn<Barcode>, HasSamples<Barcode> {
	/**
	 * Obtain the set of all compounds that the samples in this column are associated with.
	 * @return
	 */
	public String[] getCompounds();
	
	public String pack();
}
