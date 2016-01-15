/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;

/**
 * Information about a ManagedMatrix that the server maintains on behalf of the client. The main
 * purpose is to track information about columns in a matrix.
 */
@SuppressWarnings("serial")
public class ManagedMatrixInfo implements Serializable {

  private int numDataColumns = 0, numSynthetics = 0, numRows = 0;

  // Per-column information
  private List<String> columnNames = new ArrayList<String>();
  private List<String> columnHints = new ArrayList<String>();
  private List<Boolean> upperBoundFiltering = new ArrayList<Boolean>();
  private List<Group> columnGroups = new ArrayList<Group>();
  private List<ColumnFilter> columnFilters = new ArrayList<ColumnFilter>();
  private List<Boolean> isPValueColumn = new ArrayList<Boolean>();
  private List<Sample[]> samples = new ArrayList<Sample[]>();

  private List<String> platforms = new ArrayList<String>();

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
   * @param isUpperFiltering should the column have upper bound filtering? If not, lower bound is
   *        used.
   * @param baseGroup The group the column is based on
   * @param isPValue is this a p-value column?
   * @param samples The samples actually displayed in this column (may be a subset of the ones in
   *        the base group)
   */
  public void addColumn(boolean synthetic, String name, String hint, boolean isUpperFiltering,
      Group baseGroup, boolean isPValue, Sample[] samples) {
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
    this.samples.add(samples);
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

  /**
   * Add all non-synthetic columns from the other matrix into this one.
   * 
   * @param other
   */
  public ManagedMatrixInfo addAllNonSynthetic(ManagedMatrixInfo other) {
    for (int c = 0; c < other.numDataColumns(); c++) {
      addColumn(false, other.columnName(c), other.columnHint(c), other.isUpperFiltering(c),
          other.columnGroup(c), other.isPValueColumn(c), other.samples(c));
    }
    return this;
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
   * Synthetic columns are in the range numDataColumns until numColumns.
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
   * The individual filter threshold for a column, if any.
   * 
   * @param column
   * @return The filter, or null if none was set.
   */
  public @Nullable ColumnFilter columnFilter(int column) {
    return columnFilters.get(column);
  }

  public void setColumnFilter(int column, @Nullable ColumnFilter filter) {
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

  public void setAtomicProbes(String[] probes) {
    atomicProbes =
        (probes.length > ATOMICS_MAX_LENGTH ? Arrays.copyOf(probes, ATOMICS_MAX_LENGTH) : probes);
  }

  /**
   * The currently displayed probes, in order, up to a maximum limit (if too many, not all will be
   * returned)
   * 
   * @return
   */
  public String[] getAtomicProbes() {
    return atomicProbes;
  }
}
