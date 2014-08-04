package otgviewer.shared;

import t.common.shared.DataSchema;
import t.common.shared.Packable;
import t.common.shared.sample.DataColumn;
import t.common.shared.sample.HasSamples;

public interface OTGColumn extends Packable, DataColumn<OTGSample>, HasSamples<OTGSample> {
	/**
	 * Obtain the set of all compounds that the samples in this column are associated with.
	 * @return
	 */
	
	public String[] getMajors(DataSchema schema);
	
	public String pack();
}
