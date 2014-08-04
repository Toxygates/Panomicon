package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.shared.OTGSample;
import t.common.shared.SharedUtils;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

public class ChartDataset {

	protected List<ChartSample> samples;
	protected String[] categories;
	protected boolean categoriesAreTimes;
	
	protected double min = Double.NaN;
	protected double max = Double.NaN;
	
	ChartDataset(List<ChartSample> samples, List<ChartSample> allSamples, 
			String[] categories, boolean categoriesAreTimes) {
		this.samples = samples;
		this.categoriesAreTimes = categoriesAreTimes;				
		this.categories = categories;		
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
	 * Make a table corresponding to the given time or dose.
	 * If a time is given, the table will be grouped by dose.
	 * If a dose is given, the table will be grouped by time.
	 * 
	 * TODO factor out into charts.google
	 * 
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
					(s.major.equals(probeOrCompound) && !isProbe) || probeOrCompound == null) &&
					((s.minor.equals(timeOrDose) && isTime) ||
							(s.medium.equals(timeOrDose) && !isTime) ||
							timeOrDose == null)) {
				fsamples.add(s);
			}
		}
		
		makeColumns(t, fsamples);		
		return t;				
	}
	
	protected String categoryForSample(ChartSample sample) {
		for (String c : categories) {
			if (categoriesAreTimes && sample.minor.equals(c)) {
				return c;
			} else if (!categoriesAreTimes && sample.medium.equals(c)) {
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
			int cat = SharedUtils.indexOf(categories, categoryForSample(s));
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
//				if (s.call == 'P') {
//					style += "stroke-color: black";
//				} else {
//					style += "stroke-color: grey";
//				}
					
				dt.setValue(cat, col + 1, style);
				valCount[cat]++;
			}
		}
	}
}
