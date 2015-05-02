package otgviewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Information about a ManagedMatrix that the server maintains on behalf of the client. 
 * The main purpose is to track information about columns in a matrix.
 */
public class ManagedMatrixInfo implements Serializable {

	private int numDataColumns = 0, numSynthetics = 0, numRows = 0;
	private List<String> columnNames = new ArrayList<String>();
	private List<String> columnHints = new ArrayList<String>();
	private List<Boolean> upperBoundFiltering = new ArrayList<Boolean>();
	private List<Group> columnGroups = new ArrayList<Group>();
	private List<Double> columnFilters = new ArrayList<Double>();
	private List<String> platforms = new ArrayList<String>();
	private List<Boolean> isPValueColumn = new ArrayList<Boolean>();
	
	public ManagedMatrixInfo() { }
		
	public void setNumRows(int val) { numRows = val; }
	 
	
	/**
	 * Add information about a single column to this column set.
	 * @param synthetic
	 * @param name
	 * @param hint
	 * @param isUpperFiltering
	 */
	public void addColumn(boolean synthetic, String name, 
			String hint, boolean isUpperFiltering,
			Group baseGroup, boolean isPValue) {
		if (synthetic) {
			numSynthetics++;
		} else {
			numDataColumns++;
		}
		
		columnNames.add(name);
		columnHints.add(hint);
		upperBoundFiltering.add(isUpperFiltering);	
		columnGroups.add(baseGroup);
		columnFilters.add(null);
		isPValueColumn.add(isPValue);		
	}
	
	public void removeSynthetics() {
		numSynthetics = 0;
		int n = numDataColumns;
		columnNames = columnNames.subList(0, n); 
		columnHints = columnHints.subList(0, n);
		upperBoundFiltering = upperBoundFiltering.subList(0, n);	
		columnGroups = columnGroups.subList(0, n);
		columnFilters = columnFilters.subList(0, n);
	}
	
	public int numColumns() {
		return numDataColumns + numSynthetics;
	}
	
	/**
	 * Data columns are in the range #0 until numDataColumns - 1
	 * @return
	 */
	public int numDataColumns() { return numDataColumns; }
	
	/**
	 * Synthetic columns are in the range 
	 * numDataColumns until numColumns.
	 * @return
	 */
	public int numSynthetics() { return numSynthetics; }
	
	public int numRows() { return numRows; }
	
	/**
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return
	 */
	public boolean isUpperFiltering(int column) {
		return upperBoundFiltering.get(column);		
	}
	
	/**
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return The name of the column.
	 */
	public String columnName(int column) {
		return columnNames.get(column);
	}
	
	/**
	 * A human-readable description of the meaning of this column.
	 * (For tooltips)
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return
	 */
	public String columnHint(int column) {
		return columnHints.get(column);
	}
	
	/**
	 * The group that a given column was generated from, if any.
	 * @param column Column index. Must be 0 <= i < numDataColumns.
	 * @return The group that the column was generated from, or null if there is none.
	 */
	public @Nullable Group columnGroup(int column) {
		return columnGroups.get(column);
	}
	
	/**
	 * The individual filter threshold for a column, if any.
	 * @param column
	 * @return The filter, or null if none was set.
	 */
	public @Nullable Double columnFilter(int column) {
		return columnFilters.get(column);
	}
	
	public void setColumnFilter(int column, @Nullable Double filter) {
		columnFilters.set(column, filter);
	}
	
	/**
	 * Whether a given column is a p-value column.
	 * @param column column index. Must be 0 <= i < numColumns.
	 * @return
	 */
	public boolean isPValueColumn(int column) {
		return isPValueColumn.get(column);
	}
	
	public void setPlatforms(String[] platforms) { 
		this.platforms = Arrays.asList(platforms); 
	}
	
	public String[] getPlatforms() { return platforms.toArray(new String[0]); }

}
