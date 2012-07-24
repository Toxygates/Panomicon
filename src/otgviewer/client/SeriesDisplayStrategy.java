package otgviewer.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.shared.Barcode;
import otgviewer.shared.ExpressionRow;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.ScatterChart;

public abstract class SeriesDisplayStrategy {
	
	private String[] singleTimeColumns = new String[] { "3 hr", "6 hr", "9 hr", "24 hr" };
	private String[] repeatTimeColumns = new String[] { "4 day", "8 day", "15 day", "29 day" };
	
	private String[] vitroTimeColumns = new String[] { "2 hr", "8 hr", "24 hr" };

	private CoreChart chart;
	private DataTable table;
	private Barcode[] barcodes;
	private String[] individuals;
	
	public SeriesDisplayStrategy(CoreChart _chart, DataTable _table) {
		chart = _chart;
		table = _table;
	}
	
	void setupTable(Barcode[] barcodes) {
		System.out.println("Series chart got " + barcodes.length + " barcodes");
		this.barcodes = barcodes;
		table.removeColumns(0, table.getNumberOfColumns());
		table.addColumn(ColumnType.STRING, categoryName());
		Set<String> inds = new HashSet<String>();
		for (Barcode b: barcodes) {
			inds.add(b.getIndividual());
		}
		individuals = inds.toArray(new String[0]);
		Arrays.sort(individuals);
		for (String c: individuals) {
			table.addColumn(ColumnType.NUMBER, c);
		}		
		table.removeRows(0, table.getNumberOfRows());		
		int i = 0;
		for (String cat: categories()) {
			table.addRow();
			table.setValue(i,  0, cat);
			i += 1;
		}	
	}
	
	void displayData(List<ExpressionRow> data) {
		System.out.println("Series chart got " + data.size() + " rows");		
		for (ExpressionRow r: data) {
			for (int i = 0; i < barcodes.length; ++i) {
				table.setValue(categoryForBarcode(barcodes[i]),
						Arrays.binarySearch(individuals, barcodes[i].getIndividual()) + 1,
						r.getValue(i).getValue());
			}
		}
		chart.draw(table);
	}
	
	abstract int categoryForBarcode(Barcode b);
	abstract String[] categories();	
	abstract String categoryName();
	
	int indexOf(Object[] data, Object item) {
		for (int i = 0; i < data.length; ++i) {
			if (data[i].equals(item)) {
				return i;
			}
		}
		return -1;
	}
	
	public static class VsTime extends SeriesDisplayStrategy {
		public VsTime(CoreChart chart, DataTable table) {
			super(chart, table);
		}
		
		String[] categories() { return new String[] { "3 hr", "6 hr", "9 hr", "24 hr", "4 day", "8 day", "15 day", "29 day" }; }
		int categoryForBarcode(Barcode b) { return indexOf(categories(), b.getTime()); }
		String categoryName() { return "Time"; };
	}
	
	public static class VsDose extends SeriesDisplayStrategy {
		public VsDose(CoreChart chart, DataTable table) {
			super(chart, table);
		}
		
		String[] categories() { return new String[] { "Control", "Low", "Middle", "High" }; }
		int categoryForBarcode(Barcode b) { return indexOf(categories(), b.getDose()); }
		String categoryName() { return "Dose"; }
	}
}
