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

package otgviewer.client.charts;

import java.util.Arrays;

import javax.annotation.Nullable;

import otgviewer.shared.OTGSample;
import t.common.shared.DataSchema;
import t.common.shared.HasClass;
import t.common.shared.SampleClass;
import t.viewer.shared.Unit;

public class ChartSample implements HasClass {
	final SampleClass sc;
	final DataSchema schema;
	
	final double value;
	final char call;
	final @Nullable OTGSample barcode; 
	final String probe;
	String color = "grey";
	
	ChartSample(SampleClass sc, DataSchema schema,
			double value, OTGSample barcode, String probe, char call) {
		
		this.sc = sc.copyOnly(Arrays.asList(schema.chartParameters()));
		this.schema = schema;
		this.value = value;
		this.barcode = barcode;
		this.probe = probe;
		this.call = call;						 
	}

	ChartSample(OTGSample sample, DataSchema schema,
			double value, String probe, char call) {
		this(sample.sampleClass(), schema, value, sample, probe, call);					
	}
	
	ChartSample(Unit u, DataSchema schema, double value, String probe, char call) {
		this(u, schema, value, u.getSamples()[0], //representative sample only
				probe, call);			
	}
	
	public DataSchema schema() { return schema; }	
	public SampleClass sampleClass() { return sc; }	
	public double value() { return value; } 
	public OTGSample barcode() { return barcode; }
	public char call() { return call; }
	public String color() { return color; }
	public String probe() { return probe; }
	
	//TODO do we need hashCode and equals for this class?
	//See getSamples below where we use a Set<ChartSample>
	@Override
	public int hashCode() {
		int r = 0;			
		if (barcode != null) {
			r = barcode.hashCode();
		} else {
			r = r * 41 + sc.hashCode();				
			r = r * 41 + ((Double) value).hashCode();
		}
		return r;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ChartSample) {
			if (barcode != null) {
				return (barcode == ((ChartSample) other).barcode);
			} else {
				ChartSample ocs = (ChartSample) other;
				return sc.equals(((ChartSample) other).sampleClass()) &&
						ocs.value == value && ocs.call == call;
			}
		} else {
			return false;
		}
	}
}