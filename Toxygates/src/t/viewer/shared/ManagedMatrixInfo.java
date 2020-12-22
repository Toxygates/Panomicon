/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.shared;

import java.io.Serializable;
import java.util.*;

import javax.annotation.Nullable;

import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import static t.common.shared.GWTTypes.*;

/**
 * Information about a ManagedMatrix that the server maintains on behalf of the client. The main
 * purpose is to track information about columns in a matrix.
 */
@SuppressWarnings("serial")
public class ManagedMatrixInfo implements Serializable {

  private int numDataColumns = 0, numSynthetics = 0, numRows = 0;

  // Per-column information

  //Short name of a column or subcolumn. For a subcolumn this will also identify its type.
  private List<String> shortColumnNames = mkList();
  private List<String> columnNames = mkList();

  //For subcolumns, identify their parent column name. Top columns are their own parent.
  private List<String> parentColumns = mkList();
  private List<String> columnHints = mkList();
  private List<Group> columnGroups = mkList();
  private List<ColumnFilter> columnFilters = mkList();
  private List<Boolean> isPValueColumn = mkList();
  private List<Sample[]> samples = mkList();

  private List<String> platforms = mkList();

  // The currently visible probes
  private String[] atomicProbes = new String[0];
  public static final int ATOMICS_MAX_LENGTH = 1000;

  public ManagedMatrixInfo() {}

  public void setNumRows(int val) {
    numRows = val;
  }

  /**
   * Add information about a single column to this column set.
   * 
   * @param synthetic is the column synthetic?
   * @param name
   * @param hint tooltip to display
   * @param defaultFilter the default filter for this column
   * @param baseGroup The group the column is based on
   * @param isPValue is this a p-value column?
   * @param samples The samples actually displayed in this column (may be a subset of the ones in
   *        the base group)
   */
  public void addColumn(boolean synthetic, String shortName, String name, String parentName,
                        String hint, ColumnFilter defaultFilter,
      Group baseGroup, boolean isPValue, Sample[] samples) {
    if (synthetic) {
      numSynthetics++;
    } else {
      numDataColumns++;
    }

    shortColumnNames.add(shortName);
    parentColumns.add(parentName);
    columnNames.add(name);
    columnHints.add(hint);
    columnGroups.add(baseGroup);
    columnFilters.add(defaultFilter);
    isPValueColumn.add(isPValue);
    this.samples.add(samples);
  }

  public void addColumn(boolean synthetic, String name, String parent, String hint,
      ColumnFilter defaultFilter,
      Group baseGroup, boolean isPValue, Sample[] samples) {
    addColumn(synthetic, name, name, parent, hint, defaultFilter, baseGroup, isPValue, samples);
  }
  
  public void removeSynthetics() {
    numSynthetics = 0;
    int n = numDataColumns;
    // subList returns a type that can't be serialised by GWT
    shortColumnNames = mkList(shortColumnNames.subList(0, n));
    columnNames = mkList(columnNames.subList(0, n));
    parentColumns = mkList(parentColumns.subList(0, n));
    columnHints = mkList(columnHints.subList(0, n));
    columnGroups = mkList(columnGroups.subList(0, n));
    columnFilters = mkList(columnFilters.subList(0, n));
    isPValueColumn = mkList(isPValueColumn.subList(0, n));
    samples = mkList(samples.subList(0, n));
  }

  /**
   * Add all non-synthetic columns from the other matrix into this one. 
   * @param other
   */
  public ManagedMatrixInfo addAllNonSynthetic(ManagedMatrixInfo other) {
    for (int c = 0; c < other.numDataColumns(); c++) {
      addColumn(false, other.columnName(c), other.parentColumnName(c),
          other.columnHint(c), other.columnFilter(c), other.columnGroup(c),
          other.isPValueColumn(c), other.samples(c));
    }
    return this;
  }
  
  public boolean hasColumn(String name) {
    return columnNames.contains(name);
  }

  public int numColumns() {
    return numDataColumns + numSynthetics;
  }

  /**
   * Data columns are in the range #0 until numDataColumns - 1
   * 
   * @return
   */
  public int numDataColumns() {
    return numDataColumns;
  }

  /**
   * Synthetic columns are in the range numDataColumns until numColumns - 1
   * 
   * @return
   */
  public int numSynthetics() {
    return numSynthetics;
  }

  public int numRows() {
    return numRows;
  }


  /**
   * @param column Column index. Must be 0 <= i < numColumns.
   * @return The name of the column.
   */
  public String columnName(int column) {
    return columnNames.get(column);
  }

  /**
   * @param column Column index. Must be 0 <= i < numColumns.
   * @return A short name for the column.
   */
  public String shortColumnName(int column) {
    return shortColumnNames.get(column);
  }

  /**
   * @param column Column index. Must be 0 <= i < numColumns.
   * @return The name of the parent column.
   */
  public String parentColumnName(int column) { return parentColumns.get(column); }

  /**
   * A human-readable description of the meaning of this column. (For tooltips)
   * 
   * @param column Column index. Must be 0 <= i < numColumns.
   * @return
   */
  public String columnHint(int column) {
    return columnHints.get(column);
  }

  /**
   * The samples the column is based on.
   * 
   * @param column Column index. Must be 0 <= i <= numDataColumns.
   * @return
   */
  public Sample[] samples(int column) {
    return samples.get(column);
  }

  /**
   * The group that a given column was generated from, if any.
   * 
   * @param column Column index. Must be 0 <= i < numDataColumns.
   * @return The group that the column was generated from, or null if there is none.
   */
  public @Nullable Group columnGroup(int column) {
    return columnGroups.get(column);
  }

  /**
   * The individual filter for a column.
   * @param column
   * @return The filter
   */
  public ColumnFilter columnFilter(int column) {
    return columnFilters.get(column);
  }
  
  /**
   * All column filters, ordered by column.
   */
  public List<ColumnFilter> columnFilters() {
    return columnFilters;
  }

  /**
   * All column groups, ordered by column.
   */
  public List<Group> columnGroups() {
    return columnGroups;
  }

  public void setColumnFilter(int column, ColumnFilter filter) {
    columnFilters.set(column, filter);
  }

  /**
   * Whether a given column is a p-value column.
   * 
   * @param column column index. Must be 0 <= i < numColumns.
   * @return
   */
  public boolean isPValueColumn(int column) {
    return isPValueColumn.get(column);
  }

  public void setPlatforms(String[] platforms) {
    this.platforms = Arrays.asList(platforms);
  }

  public String[] getPlatforms() {
    return platforms.toArray(new String[0]);
  }

  public boolean isOrthologous() {
    return platforms.size() > 1;
  }

  public void setAtomicProbes(String[] probes) {
    atomicProbes = probes;
  }

  /**
   * The currently displayed probes, in order, up to a maximum limit (if too many, not all will be
   * returned)
   * 
   * @return
   */
  public String[] getAtomicProbes(boolean limit) {
    return ((limit && atomicProbes.length > ATOMICS_MAX_LENGTH) ?
            Arrays.copyOf(atomicProbes, ATOMICS_MAX_LENGTH) : atomicProbes);
  }

  /**
   * Searches for a column based on a hint string and group and returns its index
   */
  public int findColumn(String hint, Group group) {
    if (group == null) {
      return -1;
    }
    for (int i = 0; i < columnHints.size(); i++) {
      if (columnHints.get(i) == hint &&
          group.hasSameUnits(columnGroups.get(i))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Searches for a column based on a parent name and a short name.
   * Because short names identify column types by convention, this can also be used to
   * used to search for a sub-column by its type (e.g. P-value).
   * See constants such as ManagedMatrix.log2FoldColumnShortName for details.
   *
   * @param parentColumn Name of the parent column
   * @param shortName Short name/type of the column
   * @return The index of the column
   */
  public int findColumn(String parentColumn, @Nullable String shortName) {
    for (int i = 0; i < parentColumns.size(); i++) {
      if (parentColumns.get(i).equals(parentColumn) &&
              (shortName == null || shortColumnNames.get(i).equals(shortName))) {
        return i;
      }
    }
    return -1;
  }
}
