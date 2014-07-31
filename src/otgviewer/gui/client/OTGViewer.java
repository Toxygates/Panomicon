package otgviewer.gui.client;

import otgviewer.client.ColumnScreen;
import otgviewer.client.DataScreen;
import otgviewer.client.DatasetScreen;
import otgviewer.client.PathologyScreen;
import otgviewer.client.ProbeScreen;
import otgviewer.client.SampleDetailScreen;
import otgviewer.client.TApplication;
import otgviewer.shared.TimesDoses;
import t.common.shared.DataSchema;

/**
 * The main entry point for Toxygates.
 * The main task of this class is to manage the history mechanism and ensure that
 * the correct screen is being displayed at any given time, as well as provide a 
 * facility for inter-screen communication.
 * @author johan
 *
 */
public class OTGViewer extends TApplication {
	
	@Override
	protected void initScreens() {		
		addScreenSeq(new DatasetScreen(this));		
		addScreenSeq(new ColumnScreen(this, "Compound ranking (optional)"));		
		addScreenSeq(new ProbeScreen(this));		
		addScreenSeq(new DataScreen(this));		
		addScreenSeq(new PathologyScreen(this));
		addScreenSeq(new SampleDetailScreen(this));
	}
	
	final private TimesDoses schema = new TimesDoses();
	
	@Override
	public DataSchema schema() {
		return schema;
	}
	
	@Override
	public String storagePrefix() {
		String uit = getUIType();
		if (uit.equals("toxygates")) {
			return "OTG";
		} else {
			return "Toxy_" + uit;
		}
	}
}
