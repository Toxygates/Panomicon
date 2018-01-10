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
   * @param withPager whether navigation controls should be present
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
