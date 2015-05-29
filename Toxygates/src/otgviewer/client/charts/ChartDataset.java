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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.shared.OTGSample;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

public class ChartDataset {

	protected List<ChartSample> samples;
	protected String[] categories;
	protected boolean categoriesAreMins;
	
	protected double min = Double.NaN;
	protected double max = Double.NaN;
	
	protected Logger logger = SharedUtils.getLogger("ChartDataset");
	
	ChartDataset(List<ChartSample> samples, List<ChartSample> allSamples, 
			String[] categories, boolean categoriesAreMins) {
		this.samples = samples;
		this.categoriesAreMins = categoriesAreMins;				
		this.categories = categories;		
		logger.info(categories.length + " categories");
		init();
	}
	
	protected void init() {
		for (ChartSample s: samples) {
			if (s.value < min || Double.isNaN(min)) { 
				min = s.value;
			}
			if (s.value > max || Double.isNaN(max)) {
				max = s.value;
			}
		}		
	}
	
	/**
	 * Minimum value across the whole sample space.
	 * @return
	 */
	public double getMin() {
		return min;	
	}
	
	/**
	 * Maximum value across the whole sample space.
	 * @return
	 */
	public double getMax() {
		return max;
	}
	
	/**
	 * Get the barcode corresponding to a particular row and column.
	 * May be null.
	 * @param row
	 * @param column
	 * @return
	 */
	OTGSample getBarcode(int row, int column) {
		return null;
	}

	/**
	 * Make a table corresponding to the given filter
	 * and (optionally) probe.
	 * If a time is given, the table will be grouped by dose.
	 * If a dose is given, the table will be grouped by time.
	 * 
	 * TODO factor out into charts.google
	 * @return
	 */
	DataTable makeTable(SampleClass filter, @Nullable String probe) {
		DataTable t = DataTable.create();
		t.addColumn(ColumnType.STRING, "Time");
		
		for (int i = 0; i < categories.length; ++i) {
			t.addRow();
			t.setValue(i, 0, categories[i]);
		}		
		
		List<ChartSample> fsamples = new ArrayList<ChartSample>();
		for (ChartSample s: samples) {
			if ( (probe == null || s.probe.equals(probe)) && 
				filter.strictCompatible(s) ) {
				fsamples.add(s);
			}
		}
		
		makeColumns(t, fsamples);		
		return t;				
	}
	
	protected String categoryForSample(ChartSample sample) {
		DataSchema schema = sample.schema();
		for (String c : categories) {
			if (categoriesAreMins && schema.getMinor(sample).equals(c)) {
				return c;
			} else if (!categoriesAreMins && schema.getMedium(sample).equals(c)) {
				return c;
			}
		}
		return null;
	}

	protected native void addStyleColumn(DataTable dt) /*-{
		dt.addColumn({type:'string', role:'style'});
	}-*/;

	protected void makeColumns(DataTable dt, List<ChartSample> samples) {
		int colCount = 0;
		int[] valCount = new int[categories.length];

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
				dt.setValue(cat, col, s.value);
				if (s.barcode != null) {
					dt.setProperty(cat, col, "barcode", s.barcode.pack());
				}
				dt.setFormattedValue(cat, col, Utils.formatNumber(s.value) + ":" + s.call);
				String style = "fill-color:" + s.color + "; stroke-width:1px; ";

				dt.setValue(cat, col + 1, style);
				valCount[cat]++;
			}
		}
	}
}
