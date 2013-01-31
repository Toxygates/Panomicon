package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.Group;

import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;

abstract class ChartTables {

	protected List<ChartDataSource.ChartSample> samples;
	protected String[] categories;
	
	ChartTables(List<ChartDataSource.ChartSample> samples) {
		this.samples = samples;
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
		t.addColumn(ColumnType.NUMBER, "Value");
		
		List<String> seenCategories = new ArrayList<String>();
		int i = 0;
		//TODO sorting needed?
		for (ChartDataSource.ChartSample s: samples) {			
			String cat = isTime ? s.time : s.dose;
			if (!seenCategories.contains(cat)) {
				seenCategories.add(cat);
				t.addRow();
				t.setValue(i, 0, cat);
				i += 1;
			}		
		}
		categories = seenCategories.toArray(new String[0]);
		return t;
				
	}
	
	/**
	 * A table that puts each group in a unique column, allowing for easy
	 * color coding.
	 * @author johan
	 *
	 */
	static class GroupedChartTable extends ChartTables {
		protected List<Group> groups;
		GroupedChartTable(List<ChartDataSource.ChartSample> samples, List<Group> groups) {
			super(samples);
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
		boolean inGroup(ChartDataSource.ChartSample sample, Group group) {
			for (Barcode b: group.getBarcodes()) {
				if (b.getTime().equals(sample.time) && b.getDose().equals(sample.dose) &&
						b.getCompound().equals(sample.compound)) {
					return true;
				}						
			}
			return false;			
		}
	}
}
