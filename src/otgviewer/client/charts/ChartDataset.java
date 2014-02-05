package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.shared.Barcode;
import bioweb.shared.SharedUtils;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

public class ChartDataset {

	protected List<ChartSample> samples;
	protected String[] categories;
	protected boolean categoriesAreTimes;
	
	protected double min = Double.MAX_VALUE;
	protected double max = Double.MIN_VALUE;
	
	ChartDataset(List<ChartSample> samples, List<ChartSample> allSamples, 
			String[] categories, boolean categoriesAreTimes) {
		this.samples = samples;
		this.categoriesAreTimes = categoriesAreTimes;				
		this.categories = categories;		
		init();
	}
	
	protected void init() {
		for (ChartSample s: samples) {
			if (s.value < min) { 
				min = s.value;
			}
			if (s.value > max) {
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
	Barcode getBarcode(int row, int column) {
		return null;
	}

	/**
	 * Make a table corresponding to the given time or dose.
	 * If a time is given, the table will be grouped by dose.
	 * If a dose is given, the table will be grouped by time.
	 * @param timeOrDose
	 * @param probeOrCompound Filter samples by probe or compound 
	 * (use null for no filtering)
	 * @param isTime true iff timeOrDose is a time point.
	 * @param isCompound true iff probeOrCompound is a probe.
	 * @return
	 */
	DataTable makeTable(String timeOrDose, String probeOrCompound, boolean isTime, boolean isProbe) {
		DataTable t = DataTable.create();
		t.addColumn(ColumnType.STRING, "Time");
		
		for (int i = 0; i < categories.length; ++i) {
			t.addRow();
			t.setValue(i, 0, categories[i]);
		}		
		
		List<ChartSample> fsamples = new ArrayList<ChartSample>();
		for (ChartSample s: samples) {
			if (((s.probe.equals(probeOrCompound) && isProbe) ||
					(s.compound.equals(probeOrCompound) && !isProbe) || probeOrCompound == null) &&
					((s.time.equals(timeOrDose) && isTime) ||
							(s.dose.equals(timeOrDose) && !isTime) ||
							timeOrDose == null)) {
				fsamples.add(s);
			}
		}
		
		makeColumns(t, fsamples);		
		return t;				
	}
	
	protected String categoryForSample(ChartSample sample) {
		for (String c : categories) {
			if (categoriesAreTimes && sample.time.equals(c)) {
				return c;
			} else if (!categoriesAreTimes && sample.dose.equals(c)) {
				return c;
			}
		}
		return null;
	}

	protected native void addStyleColumn(DataTable dt) /*-{
		dt.addColumn({type:'string', role:'style'});
	}-*/;

	protected void makeColumns(DataTable dt, List<ChartSample> samples) {
		// e.g. 9 samples, 3 categories -> 3 columns
		int numColumns = samples.size() / categories.length;

		for (int i = 0; i < numColumns; ++i) {
			dt.addColumn(ColumnType.NUMBER);
			addStyleColumn(dt);
		}

		int[] valCount = new int[categories.length];

		for (ChartSample s : samples) {
			int cat = SharedUtils.indexOf(categories, categoryForSample(s));
			if (cat != -1) {
				dt.setValue(cat, valCount[cat] * 2 + 1, s.value);
				dt.setFormattedValue(cat, valCount[cat] * 2 + 1,
						Utils.formatNumber(s.value));
				dt.setValue(cat, valCount[cat] * 2 + 2, s.color);
				valCount[cat]++;
			}
		}
	}
}
