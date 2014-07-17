package t.common.shared.sample;
import t.common.shared.*;


public interface DataColumn<S extends Sample> extends HasSamples<S>, Packable {

	public String getShortTitle();
	
	public String pack();	
}
