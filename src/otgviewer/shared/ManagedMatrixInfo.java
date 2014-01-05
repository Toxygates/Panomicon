package otgviewer.shared;

import java.io.Serializable;
import java.util.Arrays;

import bioweb.shared.SharedUtils;

/**
 * Information about a ManagedMatrix that the server maintains on behalf of the client. 
 * The main purpose is to track information about columns in a matrix.
 */
public class ManagedMatrixInfo implements Serializable {

	private int _numDataColumns = 0, _numSynthetics = 0, _numRows = 0;
	private String[] columnNames = new String[0];
	private String[] columnHints = new String[0];
	private boolean[] separateFiltering = new boolean[0];
	
	public ManagedMatrixInfo() { }
	
	public void setNumRows(int val) { _numRows = val; }
	
	/**
	 * Add information about a single column to this column set.
	 * @param synthetic
	 * @param name
	 * @param hint
	 * @param isSeparateFiltering
	 */
	public void addColumn(boolean synthetic, String name, 
			String hint, boolean isSeparateFiltering) {
		if (synthetic) {
			_numSynthetics++;
		} else {
			_numDataColumns++;
		}
		
		int n = columnNames.length;
		columnNames = SharedUtils.extend(columnNames, name);
		columnHints = SharedUtils.extend(columnHints, hint);
		separateFiltering = SharedUtils.extend(separateFiltering, isSeparateFiltering);		
	}
	
	public void removeSynthetics() {
		_numSynthetics = 0;
		int n = _numDataColumns;
		columnNames = SharedUtils.take(columnNames, n);
		columnHints = SharedUtils.take(columnHints, n);
		separateFiltering = SharedUtils.take(separateFiltering, n);			
	}
	
	public int numColumns() {
		return _numDataColumns + _numSynthetics;
	}
	
	public int numDataColumns() { return _numDataColumns; }
	
	public int numSynthetics() { return _numSynthetics; }
	
	public int numRows() { return _numRows; }
	
	/**
	 * Does the column support its own separate filtering?
	 * @param column Column index. Must be 0 <= i < numColumns.
	 * @return
	 */
	public boolean hasSeparateFiltering(int column) {
		return separateFiltering[column];		
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

}
