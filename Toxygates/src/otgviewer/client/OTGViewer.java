/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import otgviewer.shared.OTGSchema;
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
		addScreenSeq(new StartScreen(this));		
		addScreenSeq(new ColumnScreen(this, "Compound ranking (optional)"));		
		addScreenSeq(new ProbeScreen(this));		
		addScreenSeq(new DataScreen(this));		
		addScreenSeq(new PathologyScreen(this));
		addScreenSeq(new SampleDetailScreen(this));
	}
	
	final private OTGSchema schema = new OTGSchema();
	
	@Override
	public DataSchema schema() {
		return schema;
	}
	
//	//TODO should use instanceName instead
//	@Override
//	@Deprecated 
//	public String storagePrefix() {
//		String uit = getUIType();
//		if (uit.equals("toxygates")) {
//			return "OTG";
//		} else {
//			return "Toxy_" + uit;
//		}
//	}
}
