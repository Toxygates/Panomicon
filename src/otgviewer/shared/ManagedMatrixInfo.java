package otgviewer.shared;

import java.io.Serializable;

import javax.annotation.Nullable;

import t.common.shared.SharedUtils;

/**
 * Information about a ManagedMatrix that the server maintains on behalf of the client. 
 * The main purpose is to track information about columns in a matrix.
 */
public class ManagedMatrixInfo implements Serializable {

	private int _numDataColumns = 0, _numSynthetics = 0, _numRows = 0;
	private String[] columnNames = new String[0];
	private String[] columnHints = new String[0];
	private boolean[] upperBoundFiltering = new boolean[0];
	private Group[] columnGroups = new Group[0];
	private Double[] columnFilters = new Double[0];
	
	public ManagedMatrixInfo() { }
	
	public void setNumRows(int val) { _numRows = val; }
	
	/**
	 * Add information about a single column to this column set.
	 * @param synthetic
	 * @param name
	 * @param hint
	 * @param isUpperFiltering
	 */
	public void addColumn(boolean synthetic, String name, 
			String hint, boolean isUpperFiltering,
			Group baseGroup) {
		if (synthetic) {
			_numSynthetics++;
		} else {
			_numDataColumns++;
		}
		
		columnNames = SharedUtils.extend(columnNames, name);
		columnHints = SharedUtils.extend(columnHints, hint);
		upperBoundFiltering = SharedUtils.extend(upperBoundFiltering, isUpperFiltering);	
		columnGroups = SharedUtils.extend(columnGroups, baseGroup);
		columnFilters = SharedUtils.extend(columnFilters, null);
	}
	
	public void removeSynthetics() {
		_numSynthetics = 0;
		int n = _numDataColumns;
		columnNames = SharedUtils.take(columnNames, n);
		columnHints = SharedUtils.take(columnHints, n);
		upperBoundFiltering = SharedUtils.take(upperBoundFiltering, n);	
		columnGroups = SharedUtils.take(columnGroups, n);
		columnFilters = SharedUtils.take(columnFilters, n);
	}
	
	public int numColumns() {
		return _numDataColumns + _numSynthetics;
	}
	
	public int numDataColumns() { return _numDataColumns; }
	
	public int numSynthetics() { return _numSynthetics; }
	
	public int numRows() { return _numRows; }
	
	/**
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return
	 */
	public boolean isUpperFiltering(int column) {
		return upperBoundFiltering[column];		
	}
	
	/**
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return The name of the column.
	 */
	public String columnName(int column) {
		return columnNames[column];
	}
	
	/**
	 * A human-readable description of the meaning of this column.
	 * (For tooltips)
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return
	 */
	public String columnHint(int column) {
		return columnHints[column];
	}
	
	/**
	 * The group that a given column was generated from, if any.
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return The group that the column was generated from, or null if there is none.
	 */
	public @Nullable Group columnGroup(int column) {
		return columnGroups[column];
	}
	
	/**
	 * The individual filter threshold for a column, if any.
	 * @param column
	 * @return The filter, or null if none was set.
	 */
	public @Nullable Double columnFilter(int column) {
		return columnFilters[column];
	}
	
	public void setColumnFilter(int column, @Nullable Double filter) {
		columnFilters[column] = filter;
	}

}
