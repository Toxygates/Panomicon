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

package otgviewer.shared;

import javax.annotation.Nullable;

import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.sample.Sample;

@SuppressWarnings("serial")
public class OTGSample extends Sample {

	public OTGSample() { super(); }
	
	public OTGSample(String _code, SampleClass _sampleClass, 
			@Nullable String controlGroup) {
		super(_code, _sampleClass, controlGroup);			 
	}
	
	public String getTitle(DataSchema schema) {
		return getShortTitle(schema) + " (" + id() + ")";
	}
	
	public String getShortTitle(DataSchema schema) {
		return get(schema.mediumParameter()) + "/" + get(schema.minorParameter());
	}

	public String toString() {
		return sampleClass.toString();
	}
	
	public static OTGSample unpack(String s) {
		String[] spl = s.split("\\$\\$\\$");
		String v = spl[0];
		if (!v.equals("Barcode_v3")) {
		  //Incorrect/legacy format - TODO: warn here
		   return null;
		} 
		String id = spl[1];
		SampleClass sc = SampleClass.unpack(spl[2]);
		return new OTGSample(id, sc, null);		
	}
	
	public String pack() {
		final String sep = "$$$";
		StringBuilder sb = new StringBuilder();
		sb.append("Barcode_v3").append(sep);
		sb.append(id()).append(sep);
		sb.append(sampleClass.pack()).append(sep);
		//Packing does not preserve controlGroup
		return sb.toString();
	}
}
