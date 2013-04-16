package otgviewer.shared;

import bioweb.shared.array.DataColumn;

public interface BarcodeColumn extends DataColumn<Barcode> {
	public String[] getCompounds();
}
