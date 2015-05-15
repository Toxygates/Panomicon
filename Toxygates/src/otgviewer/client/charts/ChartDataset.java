package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.shared.OTGSample;
import t.common.shared.DataSchema;
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
	 * @param organism organism to filter by (or null for no filtering)
	 * @return
	 */
	DataTable makeTable(String timeOrDose, boolean isTime, 
			@Nullable String probeOrCompound, boolean isProbe,
			@Nullable String organism) {
		DataTable t = DataTable.create();
		t.addColumn(ColumnType.STRING, "Time");
		
		for (int i = 0; i < categories.length; ++i) {
			t.addRow();
			t.setValue(i, 0, categories[i]);
		}		
		
		List<ChartSample> fsamples = new ArrayList<ChartSample>();
		DataSchema schema = samples.get(0).schema();
		for (ChartSample s: samples) {
			if (
				((s.probe.equals(probeOrCompound) && isProbe) ||
						(probeOrCompound == null || 
						schema.getMajor(s).equals(probeOrCompound) && !isProbe)) &&
				((schema.getMinor(s).equals(timeOrDose) && isTime) ||
						(timeOrDose == null || schema.getMedium(s).equals(timeOrDose) && !isTime)) &&
						(organism == null || s.sampleClass().get("organism").equals(organism))) {
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
