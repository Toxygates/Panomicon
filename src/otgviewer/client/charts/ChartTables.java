package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.Utils;
import otgviewer.shared.Barcode;
import otgviewer.shared.Group;
import otgviewer.shared.SharedUtils;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

abstract class ChartTables {

	protected List<ChartDataSource.ChartSample> samples;
	protected String[] categories;
	protected boolean categoriesAreTimes;
	
	protected double min = Double.MAX_VALUE;
	protected double max = Double.MIN_VALUE;
	
	ChartTables(List<ChartDataSource.ChartSample> samples, List<ChartDataSource.ChartSample> allSamples, 
			String[] categories, boolean categoriesAreTimes) {
		this.samples = samples;
		this.categoriesAreTimes = categoriesAreTimes;		
		
		for (ChartDataSource.ChartSample s: allSamples) {
			if (s.value < min) { 
				min = s.value;
			}
			if (s.value > max) {
				max = s.value;
			}
		}
		
		this.categories = categories;		
	}
	
	/**
	 * Minimum value across the whole sample space.
	 * @return
	 */
	double getMin() {
		return min;	
	}
	
	/**
	 * Maximum value across the whole sample space.
	 * @return
	 */
	double getMax() {
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
		
		List<ChartDataSource.ChartSample> fsamples = new ArrayList<ChartDataSource.ChartSample>();
		for (ChartDataSource.ChartSample s: samples) {
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
	
	
	protected String categoryForSample(ChartDataSource.ChartSample sample) {
		for (String c: categories) {
			if (categoriesAreTimes && sample.time.equals(c)) {
				return c;
			} else if (!categoriesAreTimes && sample.dose.equals(c)) {
				return c;
			}
		}
		return null;
	}
	
	
	protected abstract void makeColumns(DataTable dt, List<ChartDataSource.ChartSample> samples);
	
	static class PlainChartTable extends ChartTables {
		
		public PlainChartTable(List<ChartDataSource.ChartSample> samples, List<ChartDataSource.ChartSample> allSamples,
				String[] categories, boolean categoriesAreTimes) {
			super(samples, allSamples, categories, categoriesAreTimes);
		}
		
		protected void makeColumns(DataTable dt, List<ChartDataSource.ChartSample> samples) {
			int nc = samples.size() / categories.length;
			for (int i = 0; i < nc; ++i) {
				dt.addColumn(ColumnType.NUMBER);
				dt.setProperty(0, i + 1, "color", "LightSkyBlue");
			}
			int[] valCount = new int[categories.length];
			
			for (ChartDataSource.ChartSample s: samples) {
				int cat = SharedUtils.indexOf(categories, categoryForSample(s));
				if (cat != -1) {
					dt.setValue(cat, valCount[cat] + 1, s.value);
					dt.setFormattedValue(cat, valCount[cat] + 1,
							Utils.formatNumber(s.value));
					valCount[cat]++;
				}				
			}			
		}
	}
	
	/**
	 * A table that puts each group in a unique column, allowing for easy
	 * color coding.
	 * @author johan
	 *
	 */
	static class GroupedChartTable extends ChartTables {
		private static class TableColumn {
			Group group; //can be null for the default group
			
			String color() {
				if (group == null) {
					return "DarkGrey";
				} else {
					return group.getColor();
				}
			}
			
			ChartDataSource.ChartSample[] samples; 			
			
			/**
			 * @param categories number of times/doses
			 */
			TableColumn(int categories) {				
				samples = new ChartDataSource.ChartSample[categories];				
			}
		}
		
	
		protected List<Group> groups;		
		
		GroupedChartTable(List<ChartDataSource.ChartSample> samples,
				List<ChartDataSource.ChartSample> allSamples, 
				List<Group> groups, 
				String[] categories, boolean categoriesAreTimes) {
			super(samples, allSamples, categories, categoriesAreTimes);
//			Window.alert(samples.size() + " ");
			this.groups = groups;			
				
		}
		
		/**
		 * Test whether the given sample belongs to a given group.
		 * NB: species, cell type and repeat type are not tested.
		 * Only time, dose and compound attributes are tested.
		 * @param sample
		 * @param group
		 * @return
		 */
		private boolean inGroup(ChartDataSource.ChartSample sample, Group group) {
			for (Barcode b: group.getBarcodes()) {
				if (b.getTime().equals(sample.time) && b.getDose().equals(sample.dose) &&
						b.getCompound().equals(sample.compound)) {
					return true;
				}						
			}
			return false;			
		}
		
		private Group groupForSample(ChartDataSource.ChartSample sample) {
			for (Group g: groups) {
				if (inGroup(sample, g)) {
					return g;
				}
			}
			return null;
		}

		
		protected void makeColumns(DataTable dt, List<ChartDataSource.ChartSample> samples) {
			List<TableColumn> tableColumns = new ArrayList<TableColumn>(); //all columns
			List<TableColumn> defaultColumns = new ArrayList<TableColumn>(); //columns for grey, group-less samples
			
			Map<Group, TableColumn> groupColumns = new HashMap<Group, TableColumn>();
			
			TableColumn defaultColumn = new TableColumn(categories.length);
			tableColumns.add(defaultColumn);
			defaultColumns.add(defaultColumn);
			
			for (ChartDataSource.ChartSample s: samples) {
				Group g = groupForSample(s);
				String c = categoryForSample(s);
				int ic = SharedUtils.indexOf(categories, c);
				
				TableColumn tc = defaultColumn;
				if (g != null) {
					if (groupColumns.containsKey(g) && groupColumns.get(g).samples[ic] == null) {
						tc = groupColumns.get(g);					
					} else {
						tc = new TableColumn(categories.length);
						tableColumns.add(tc);
						tc.group = g;
						groupColumns.put(g, tc);
					}
					
				} else if (ic != -1){
					if (tc.samples[ic] != null) {
						//unable to use the most recent default column
						
						TableColumn found = null;
						for (TableColumn tci: defaultColumns) {
							//try to find another default column we can use
							if (tci.samples[ic] == null) {
								tc = found = tci;
								break;
							}
						}
						if (found == null) {
							//need to make a new default column
							tc = defaultColumn = new TableColumn(categories.length);
							defaultColumns.add(tc);
							tableColumns.add(tc);
						} 
						
					}
				}
				
				if (ic!= -1) {
					tc.samples[ic] = s;
				}				
			}
			
			//Place group columns last
			Collections.sort(tableColumns, new Comparator<TableColumn>() {
				@Override
				public int compare(TableColumn arg0, TableColumn arg1) {
					if (arg0.group == null) {
						if (arg1.group == null) {
							return 0;
						} else {
							return 1;									
						}						
					} else {
						if (arg1.group == null) {
							return -1;
						} else {
							return 0;
						}
					}
				}
				
			});
			
			for (int c = 0; c < tableColumns.size(); ++ c) {
				dt.addColumn(ColumnType.NUMBER);				
				TableColumn tc = tableColumns.get(c);
				for (int i = 0; i < tc.samples.length; ++i) {
					if (tc.samples[i] != null) {
						dt.setValue(i, c + 1, tc.samples[i].value);
						dt.setFormattedValue(i, c + 1,
								Utils.formatNumber(tc.samples[i].value));
						
						dt.setProperty(i, c+1, "barcode", tc.samples[i].barcode.pack());
					}
				}
				dt.setProperty(0, c+1, "color", tc.color());
			}
		
		}		
	}
}
