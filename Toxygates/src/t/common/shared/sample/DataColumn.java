package t.common.shared.sample;
import t.common.shared.*;
import t.viewer.shared.DataSchema;


public interface DataColumn<S extends Sample> extends HasSamples<S>, Packable {

	public String getShortTitle(DataSchema schema);
	
	public String pack();	
}
