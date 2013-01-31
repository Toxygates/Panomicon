package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.Utils;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.SampleTimes;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

public abstract class SeriesDisplayStrategy {

	protected DataTable table;
	protected Barcode[] barcodes;
//	protected int[][] bcTable;
	protected String[] individuals;
	protected List<Group> groups;
	private Screen screen;
	
	private class TableColumn {
		Group group; //can be null for the default group
		
		String colour() {
			if (group == null) {
				return "DarkGrey";
			} else {
				return group.getColour();
			}
		}
		
		/**
		 * May be sparsely populated. Each offset corresponds to a category.
		 */
		int[] bcIndex;
		Barcode[] barcodes;
		
		/**
		 * @param categories number of times/doses
		 */
		TableColumn(int categories) {
			bcIndex = new int[categories];
			Arrays.fill(bcIndex, -1);
			barcodes = new Barcode[categories];
		}
	}
	
	protected List<TableColumn> tableColumns = new ArrayList<TableColumn>();
	protected Map<Group, TableColumn> groupColumns = new HashMap<Group, TableColumn>();
	
	TableColumn defaultColumn;
	
	public SeriesDisplayStrategy(Screen _screen, List<Group> _groups, DataTable _table) {		
		table = _table;
		screen = _screen;
		groups = _groups;
	}
	
	void setupTable(Barcode[] barcodes) {
		System.out.println("Series chart got " + barcodes.length + " barcodes");
		this.barcodes = barcodes;
		final int nc = categories().length;
		defaultColumn = new TableColumn(nc);
	
		table.removeColumns(0, table.getNumberOfColumns());
		table.addColumn(ColumnType.STRING, categoryName());
		
		for (int x = 0; x < barcodes.length; ++x) {
			int cat = categoryForBarcode(barcodes[x]);
			Group g = groupForBarcode(barcodes[x]);
			TableColumn tc = defaultColumn;
			if (g != null) {
				if (groupColumns.containsKey(g) && groupColumns.get(g).barcodes[cat] == null) {
					tc = groupColumns.get(g);					
				} else {
					tc = new TableColumn(nc);
					tableColumns.add(tc);
					tc.group = g;
					groupColumns.put(g, tc);
				}
				
			} else {
				if (tc.barcodes[cat] != null) {
					tc = defaultColumn = new TableColumn(nc);
					tableColumns.add(tc);
				}
			}
			if (cat != -1) {
				tc.bcIndex[cat] = x;
				tc.barcodes[cat] = barcodes[x];
			}
					
		}
//		
		for (TableColumn tc: tableColumns) {
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
	
	void displayData(List<ExpressionRow> data, final CoreChart chart) {
//		System.out.println("Series chart got " + data.size() + " rows");
		
		for (int c = 0; c < tableColumns.size(); ++ c) {
			TableColumn tc = tableColumns.get(c);
			for (ExpressionRow r : data) { //most of the time we actually expect a single row
				for (int i = 0; i < tc.bcIndex.length; ++i) {
					if (tc.bcIndex[i] != -1) {
						double v = r.getValue(tc.bcIndex[i]).getValue();						
						table.setValue(i, c + 1, v);
						table.setFormattedValue(i, c + 1, Utils.formatNumber(v));
					}
				}
			}
		}
		
		chart.draw(table, Utils.createChartOptions(getColumnColors()));
		chart.addSelectHandler(new SelectHandler() {			
			@Override
			public void onSelect(SelectEvent event) {
				JsArray<Selection> ss = chart.getSelections();
				Selection s = ss.get(0);
				int col = s.getColumn();
				int row = s.getRow();
				TableColumn tc = tableColumns.get(col - 1);				
				Barcode b = tc.barcodes[row];
				screen.displaySampleDetail(b);
			}
		});
	}
	
	abstract int categoryForBarcode(Barcode b);
	abstract String[] categories();	
	abstract String categoryName();
	abstract CoreChart makeChart();
	
	Group groupForBarcode(Barcode b) {
		return Utils.groupFor(groups, b.getCode());
	}
	
	int indexOf(Object[] data, Object item) {
		for (int i = 0; i < data.length; ++i) {
			if (data[i].equals(item)) {
				return i;
			}
		}
		return -1;
	}
	
	String[] getColumnColors() {
		String[] colors = new String[tableColumns.size()];
		for (int i = 0; i < tableColumns.size(); ++i) {
			colors[i] = tableColumns.get(i).colour();
		}
		return colors;
//		return new String[] { "red", "blue" };
	}
	
	
	public static class VsTime extends SeriesDisplayStrategy {
		public VsTime(Screen screen, List<Group> groups, DataTable table) {
			super(screen, groups, table);
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
			Options o = Utils.createChartOptions(getColumnColors());
			return new ColumnChart(table, o);
		}
	}
	
	public static class VsDose extends SeriesDisplayStrategy {
		public VsDose(Screen screen, List<Group> groups, DataTable table) {
			super(screen, groups, table);
		}
		
		public static final String[] allDoses = new String[] { "Control", "Low", "Middle", "High" };
		String[] categories() { return allDoses; }
		int categoryForBarcode(Barcode b) { return indexOf(categories(), b.getDose()); }
		String categoryName() { return "Dose"; }
		CoreChart makeChart() {
			Options o = Utils.createChartOptions(getColumnColors());			
			return new ColumnChart(table, o);
		}
	}
}
