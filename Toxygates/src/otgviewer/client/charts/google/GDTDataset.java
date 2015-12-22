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

package otgviewer.client.charts.google;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.charts.ChartSample;
import otgviewer.client.charts.Dataset;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

public class GDTDataset extends Dataset<GDTData> {

	GDTDataset(List<ChartSample> samples, List<ChartSample> allSamples, String[] categories,
			boolean categoriesAreMins) {
		super(samples, allSamples, categories, categoriesAreMins);
	}

	@Override
	public GDTData makeData(SampleClass filter, @Nullable String probe) {
		DataTable t = DataTable.create();
		t.addColumn(ColumnType.STRING, "Time");
		
		for (int i = 0; i < categories.length; ++i) {
			t.addRow();
			t.setValue(i, 0, categories[i]);
		}		
		
		List<ChartSample> fsamples = new ArrayList<ChartSample>();
		for (ChartSample s: samples) {
			if ( (probe == null || s.probe().equals(probe)) && 
				filter.strictCompatible(s) ) {
				fsamples.add(s);
			}
		}
		
		GDTData gdt = new GDTData(t);
		makeColumns(gdt, fsamples);		
		return gdt;				
	}
	
	protected void makeColumns(GDTData gdt, List<ChartSample> samples) {	
		int colCount = 0;
		int[] valCount = new int[categories.length];
		DataTable dt = gdt.data();

		for (ChartSample s : samples) {
			String scat = categoryForSample(s);
			int cat = SharedUtils.indexOf(categories, scat);
			if (cat != -1) {
				if (colCount < valCount[cat] + 1) {
					dt.addColumn(ColumnType.NUMBER);
					addStyleColumn(dt);
					colCount++;
				}
				
				final int col = valCount[cat] * 2 + 1;
				dt.setValue(cat, col, s.value());
				if (s.sample() != null) {
					dt.setProperty(cat, col, "barcode", s.sample().pack());
				}
				dt.setFormattedValue(cat, col, s.formattedValue());
				String style = "fill-color:" + s.color() + "; stroke-width:1px; ";

				dt.setValue(cat, col + 1, style);
				valCount[cat]++;
			}
		}
	}

	protected native void addStyleColumn(DataTable dt) /*-{
		dt.addColumn({type:'string', role:'style'});
	}-*/;

}
