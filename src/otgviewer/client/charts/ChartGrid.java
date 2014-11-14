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
 */
abstract public class ChartGrid extends Composite {
	
	private final SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	
	Grid g;
		
	List<String> rowFilters;
	List<String> organisms;
	String[] minsOrMeds;
	protected ChartDataset table;
	protected Screen screen;	
//	Map<String, DataTable> tables = new HashMap<String, DataTable>(); //TODO
	protected DataTable[][] tables;
	final int totalWidth;
	
	/**
	 * 
	 * @param screen
	 * @param table
	 * @param rowFilters major parameter values or gene symbols.
	 * @param rowsAreMajors are rows major parameter values? If not, they are gene symbols.
	 * @param minsOrMeds
	 * @param columnsAreMins
	 * @param totalWidth
	 */
	public ChartGrid(Screen screen, ChartDataset table, 
			final List<String> rowFilters, final List<String> organisms,
			boolean rowsAreMajors, 
			String[] minsOrMeds, boolean columnsAreMins, int totalWidth) {
		super();
		this.rowFilters = rowFilters;		
		this.organisms = organisms;
		this.minsOrMeds = minsOrMeds;
		this.table = table;		
		this.totalWidth = totalWidth;
		this.screen = screen;
		
		final int osize = organisms.size();
		final int rfsize = rowFilters.size();
		g = new Grid(rfsize * osize * 2 + 1, minsOrMeds.length);		
		initWidget(g);
	
		tables = new DataTable[rfsize * osize][minsOrMeds.length];
		for (int c = 0; c < minsOrMeds.length; ++c) {
			g.setWidget(0, c, Utils.mkEmphLabel(minsOrMeds[c]));				
			for (int r = 0; r < rfsize; ++r) {				
				for (int o = 0; o < osize; ++o){
				tables[r * osize + o][c] = table.makeTable(minsOrMeds[c], columnsAreMins,
						rowFilters.get(r), !rowsAreMajors, 
						organisms.get(o)); //TODO pass organism
				}
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
		for (int r = 0; r < tables.length; ++r) {
			for (int c = 0; c < tables[0].length; ++c) {						
				if (tables[r][c].getNumberOfColumns() > max) {
					max = tables[r][c].getNumberOfColumns();
				}				
			}
		}
		return max;
	}

	public void adjustAndDisplay(int tableColumnCount, double minVal, double maxVal) {
		final int width = totalWidth / minsOrMeds.length; //width of each individual chart
		final int osize = organisms.size();
		for (int c = 0; c < minsOrMeds.length; ++c) {						
			for (int r = 0; r < rowFilters.size(); ++r) {					
				for (int o = 0; o < osize; ++o) {
					String label = organisms.get(o) + ":" + rowFilters.get(r);
					displaySeriesAt(r * osize + o, c, width, minVal, maxVal, 
							tableColumnCount, label);
				}
			}
		}
			/*	
		for (int r = 2; r < g.getRowCount(); r += 2) {
			boolean hasChart = false;
			for (int c = 0; c < minsOrMeds.length; ++c) {
				if (g.getWidget(r, c) != null) {
					hasChart = true;
				}
			}
			if (!hasChart) {
				//remove empty rows
				g.removeRow(r - 1);
				g.removeRow(r);
				r -= 2; //repeat from same r
			}
		}*/
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
	private void displaySeriesAt(int row, int column, int width, double minVal,
			double maxVal, int columnCount, String label) {
		final DataTable dt = tables[row][column];

		if (dt.getNumberOfColumns() == 1) {			
			return; //no data columns -> no data to show
		}
		if (g.getWidget(row * 2 + 1, 0) == null)
		{
			//add the label if this is the first chart for the rowFilter
			g.setWidget(row * 2 + 1, 0, Utils.mkEmphLabel(label));
		}
		g.setWidget(row * 2 + 2, column,
				chartFor(dt, width, minVal, maxVal, column, columnCount));
	}
	
	abstract protected Widget chartFor(final DataTable dt, int width, double minVal, double maxVal, 
			int column, int columnCount); 
}
