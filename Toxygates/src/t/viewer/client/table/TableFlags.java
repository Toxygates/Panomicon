package t.viewer.client.table;

import javax.annotation.Nullable;

/**
 * Controls the behaviour of an ExpressionTable.
 */
public class TableFlags {
  final boolean withPValueOption, withPager, allowHighlight;
  final @Nullable String title;
  final String matrixId;
  final int initPageSize;
  
  public TableFlags(String matrixId, boolean withPValueOption, 
      boolean withPager,
      int initPageSize, 
      @Nullable String title,
      boolean allowHighlight) {
    this.withPValueOption = withPValueOption;
    this.withPager = withPager;
    this.title = title;
    this.matrixId = matrixId;
    this.initPageSize = initPageSize;
    this.allowHighlight = allowHighlight;
  }
}
