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

package t.viewer.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.shared.OTGSample;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;

/**
 * A sample class with associated samples.
 * 
 * TODO: generify, split up OTG/non-OTG versions
 * @author johan
 */
public class Unit extends SampleClass {

	private OTGSample[] samples;
	
	public Unit() {
	}
	
	public Unit(SampleClass sc, OTGSample[] samples) {
		super(sc.getMap());
		this.samples = samples;
	}

	public OTGSample[] getSamples() {
		return samples;
	}
	
	public static Unit[] formUnits(DataSchema schema, OTGSample[] samples) {
		if (samples.length == 0) {
			return new Unit[] {};
		}		
		Map<SampleClass, List<OTGSample>> groups = 
				new HashMap<SampleClass, List<OTGSample>>();
		for (OTGSample os: samples) {
			SampleClass unit = os.sampleClass().asUnit(schema);
			if (!groups.containsKey(unit)) {
				groups.put(unit, new ArrayList<OTGSample>());				
			}
			groups.get(unit).add(os);
		}
		
		List<Unit> r = new ArrayList<Unit>();
		for (SampleClass u: groups.keySet()) {
			Unit uu = new Unit(u, groups.get(u).toArray(new OTGSample[] {}));
			r.add(uu);
		}
		return r.toArray(new Unit[]{});
	}
	
	public static OTGSample[] collectBarcodes(Unit[] units) {
		List<OTGSample> r = new ArrayList<OTGSample>();
		for (Unit b: units) {
			Collections.addAll(r, b.getSamples());		
		}
		return r.toArray(new OTGSample[0]);
	}
	

	public static boolean contains(Unit[] units, String param, String value) {
		return SampleClass.filter(units, param, value).size() > 0;		
	}
}
