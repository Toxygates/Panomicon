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

package t.viewer.client.table;

import javax.annotation.Nullable;

/**
 * Controls the behaviour of an ExpressionTable.
 */
public class TableFlags {
  final boolean withPValueOption, withPager, allowHighlight,
    keepSortOnReload;
  final @Nullable String title;
  final String matrixId;
  final int initPageSize;
  
  /**
   * Construct table flags.
   * 
   * @param matrixId ID of the matrix being displayed (for server communication)
   * @param withPValueOption
   * @param withPager whether navigation controls should be present (note: this flag is 
   * currently not used anywyhere - we always construct at least an invisible pager.)
   * @param initPageSize initial page size
   * @param title human-readable label to display
   * @param allowHighlight allow highlighting rows?
   * @param keepSortOnReload keep sort order when a new matrix is loaded?
   */
  public TableFlags(String matrixId, boolean withPValueOption, 
      boolean withPager,
      int initPageSize, 
      @Nullable String title,
      boolean allowHighlight,
      boolean keepSortOnReload) {
    this.withPValueOption = withPValueOption;
    this.withPager = withPager;
    this.title = title;
    this.matrixId = matrixId;
    this.initPageSize = initPageSize;
    this.allowHighlight = allowHighlight;
    this.keepSortOnReload = keepSortOnReload;
  }
}
