package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.SampleTimes;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

public abstract class SeriesDisplayStrategy {

	protected DataTable table;
	protected Barcode[] barcodes;
	protected int[][] bcTable;
	protected String[] individuals;
	
	public SeriesDisplayStrategy(DataTable _table) {		
		table = _table;
	}
	
	void setupTable(Barcode[] barcodes) {
		System.out.println("Series chart got " + barcodes.length + " barcodes");
		this.barcodes = barcodes;
		bcTable = new int[categories().length][]; //rows
		int cols = barcodes.length/categories().length;
		for (int i = 0; i < bcTable.length; ++i) {
			bcTable[i] = new int[cols];		
			Arrays.fill(bcTable[i], -1);
		}		
		
		table.removeColumns(0, table.getNumberOfColumns());
		table.addColumn(ColumnType.STRING, categoryName());
		
		for (int x = 0; x < barcodes.length; ++x) {
			int cat = categoryForBarcode(barcodes[x]);
			if (cat != -1) {
				for (int i = 0; i < bcTable[cat].length; ++i) {
					if (bcTable[cat][i] == -1) {
						bcTable[cat][i] = x;
						break;
					}
				}
			}			
		}
		
		
		for (int x: bcTable[0]) {
			table.addColumn(ColumnType.NUMBER);
		}		
		
		table.removeRows(0, table.getNumberOfRows());		
		int i = 0;
		for (String cat: categories()) {
			table.addRow();
			table.setValue(i,  0, cat);
			i += 1;
		}	
	}
	
	void displayData(List<ExpressionRow> data, CoreChart chart) {
//		System.out.println("Series chart got " + data.size() + " rows");
		
		if (bcTable.length > 0) {	
			for (ExpressionRow r : data) {
				for (int i = 0; i < bcTable.length; ++i) {
					for (int j = 0; j < bcTable[i].length; ++j) {
						if (bcTable[i][j] != -1) {
							double v = r.getValue(bcTable[i][j]).getValue();
							
							table.setValue(i, j + 1, v);
							table.setFormattedValue(i, j + 1, Utils.formatNumber(v));
						}
					}
				}
			}
		}		
		chart.draw(table, Utils.createChartOptions("MediumAquaMarine"));
	}
	
	abstract int categoryForBarcode(Barcode b);
	abstract String[] categories();	
	abstract String categoryName();
	abstract CoreChart makeChart();
	
	int indexOf(Object[] data, Object item) {
		for (int i = 0; i < data.length; ++i) {
			if (data[i].equals(item)) {
				return i;
			}
		}
		return -1;
	}
	
	
	public static class VsTime extends SeriesDisplayStrategy {
		public VsTime(DataTable table) {
			super(table);
		}
		private String[] categorySubset; 
		
		public final static String[] allTimes = SampleTimes.allTimes;		
		private String[] allCategories = allTimes; 
		
		int categoryForBarcode(Barcode b) { return indexOf(categorySubset, b.getTime()); }
		
		private boolean hasBarcodeForCategory(int cat) {			
			for (Barcode b : barcodes) {
				if (b.getTime().equals(allCategories[cat])) {
					return true;
				}
			}
			return false;
		}
		
		String[] categories() {
			ArrayList<String> r = new ArrayList<String>();
			for (int i = 0; i < allCategories.length; ++i) {
				if (hasBarcodeForCategory(i)) {
					r.add(allCategories[i]);
				}
			}
			categorySubset = r.toArray(new String[0]);
			return categorySubset;
		}
		
		String categoryName() { return "Time"; };
		CoreChart makeChart() {
			Options o = Utils.createChartOptions("MediumAquaMarine");
			return new ColumnChart(table, o);
		}
	}
	
	public static class VsDose extends SeriesDisplayStrategy {
		public VsDose(DataTable table) {
			super(table);
		}
		
		public static final String[] allDoses = new String[] { "Control", "Low", "Middle", "High" };
		String[] categories() { return allDoses; }
		int categoryForBarcode(Barcode b) { return indexOf(categories(), b.getDose()); }
		String categoryName() { return "Dose"; }
		CoreChart makeChart() {
			Options o = Utils.createChartOptions("MediumAquaMarine");			
			return new ColumnChart(table, o);
		}
	}
}
