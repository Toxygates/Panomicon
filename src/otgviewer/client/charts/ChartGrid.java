package otgviewer.client.charts;

import java.util.List;

import otgviewer.client.Utils;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.rpc.SparqlService;
import otgviewer.client.rpc.SparqlServiceAsync;
import otgviewer.shared.Group;
import t.common.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.DataTable;

/**
 * A grid to display time series charts for a number of probes and doses.
 * @author johan
 *
 */
abstract public class ChartGrid extends Composite {
	
	private final SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	
	Grid g;
	
	boolean rowsAreMajors, columnsAreMins;
	List<String> rowFilters;
	String[] minsOrMeds;
	protected ChartDataset table;
	protected Screen screen;	
	protected DataTable[][] tables;
	final int totalWidth;
	
	public ChartGrid(Screen screen, ChartDataset table, List<Group> groups, 
			final List<String> rowFilters, boolean rowsAreMajors, 
			String[] minsOrMeds, boolean columnsAreMins, int totalWidth) {
		super();
		this.rowFilters = rowFilters;
		this.rowsAreMajors = rowsAreMajors;
		this.minsOrMeds = minsOrMeds;
		this.table = table;
		this.columnsAreMins = columnsAreMins;
		this.totalWidth = totalWidth;
		this.screen = screen;
		
		g = new Grid(rowFilters.size() * 2 + 1, minsOrMeds.length);		
		initWidget(g);
		
		for (int r = 0; r < rowFilters.size(); ++r) {
			g.setWidget(r * 2 + 1, 0, Utils.mkEmphLabel(rowFilters.get(r)));
		}
		
		tables = new DataTable[rowFilters.size()][minsOrMeds.length];
		for (int c = 0; c < minsOrMeds.length; ++c) {
			g.setWidget(0, c, Utils.mkEmphLabel(minsOrMeds[c]));				
			for (int r = 0; r < rowFilters.size(); ++r) {
				tables[r][c] = table.makeTable(minsOrMeds[c], rowFilters.get(r), 
						columnsAreMins, !rowsAreMajors);
				
			}
		}
	
		if (!rowsAreMajors) {
			owlimService.geneSyms(rowFilters.toArray(new String[0]),
				new PendingAsyncCallback<String[][]>(screen) {
					public void handleSuccess(String[][] results) {
						for (int i = 0; i < results.length; ++i) {
							g.setWidget(
									i * 2 + 1, 0,
									Utils.mkEmphLabel(SharedUtils.mkString(results[i]) + "/" + rowFilters.get(i)));										
						}
					}
				});
		}

	}
	
	public int computedTotalWidth() {
		int theoretical = g.getColumnCount() * GVizChartGrid.MAX_WIDTH;
		if (theoretical > totalWidth) {
			return totalWidth;
		} else {
			return theoretical;
		}
	}
	
	/**
	 * Obtain the largest number of data columns used in any of our backing tables.
	 * @return
	 */
	public int getMaxColumnCount() {
		int max = 0;
		for (int c = 0; c < minsOrMeds.length; ++c) {						
			for (int r = 0; r < rowFilters.size(); ++r) {
				if (tables[r][c].getNumberOfColumns() > max) {
					max = tables[r][c].getNumberOfColumns();
				}				
			}
		}
		return max;
	}

	public void adjustAndDisplay(int tableColumnCount, double minVal, double maxVal) {
		final int width = totalWidth / minsOrMeds.length; //width of each individual chart 		
		for (int c = 0; c < minsOrMeds.length; ++c) {						
			for (int r = 0; r < rowFilters.size(); ++r) {							
				displaySeriesAt(r, c, width, minVal, maxVal, tableColumnCount);
			}
		}
	}
	
	/**
	 * We normalise the column count of each data table when displaying it
	 * in order to force the charts to have equally wide bars.
	 * (To the greatest extent possible)
	 * @param row
	 * @param column
	 * @param width
	 * @param columnCount
	 */
	private void displaySeriesAt(int row, int column, int width, double minVal, double maxVal, int columnCount) {
		final DataTable dt = tables[row][column];	
		g.setWidget(row * 2 + 2, column, chartFor(dt, width, minVal, maxVal, column, columnCount));
	}
	
	abstract protected Widget chartFor(final DataTable dt, int width, double minVal, double maxVal, 
			int column, int columnCount); 
}
