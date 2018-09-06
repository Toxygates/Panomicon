package t.viewer.client.table;

import java.util.List;

import com.google.gwt.dom.builder.shared.SpanBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;

import t.common.shared.DataSchema;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.shared.ManagedMatrixInfo;

/**
 * Implementation of HeaderBuilder for ExpressionTable, subclassed in order to
 * build group headers: column headers that sit above the individual columns
 * headers, spanning all columns belonging to a sample group.
 * 
 * This serves to visually represent the grouping of these columns. Also, the
 * name of the sample group can be put in the group header, leaving more room in
 * individual column headers for other information.
 */
public class ETHeaderBuilder extends DefaultHeaderOrFooterBuilder<ExpressionRow> {
  AbstractCellTable.Style style;
  private Delegate delegate;
  DataSchema schema;

  public interface Delegate {
    List<String> columnSections();
    int columnCountForSection(String sectionName);
    boolean displayPColumns();
    ManagedMatrixInfo matrixInfo();
  }

  public ETHeaderBuilder(AbstractCellTable<ExpressionRow> table,
      Delegate delegate, DataSchema schema) {
    super(table, false);
    this.delegate = delegate;
    this.schema = schema;
    style = getTable().getResources().style();
  }

  /**
   * Builds a group header for a number of columns related to a given sample
   * group.
   */
  private void buildGroupHeader(TableRowBuilder rowBuilder, Group group, int columnCount,
      String styleNames) {
    SpanBuilder spanBuilder = rowBuilder.startTH().colSpan(columnCount)
        .className(style.header() + " majorHeader " + styleNames).startSpan();
    spanBuilder.title(group.tooltipText(schema)).text(group.getName()).endSpan();
    rowBuilder.endTH();
  }

  /**
   * Creates an empty group header for some number of columns. These invisible
   * headers are used to pad the space above columns that should not have group
   * headers.
   */
  private void buildBlankHeader(TableRowBuilder rowBuilder, int columnCount) {
    rowBuilder.startTH().colSpan(columnCount).endTH();
  }

  @Override
  protected boolean buildHeaderOrFooterImpl() {
    ManagedMatrixInfo matrixInfo = delegate.matrixInfo();

    if (delegate.columnSections().size() > 0) {
      TableRowBuilder rowBuilder = startRow();
      for (String sectionName : delegate.columnSections()) {
        int numSectionColumns = delegate.columnCountForSection(sectionName);
        if (numSectionColumns > 0 && matrixInfo != null) {
          if (sectionName == "data") {
            int groupColumnCount = 1;
            Group group = matrixInfo.columnGroup(0);
            // Iterate through data columns, and build a group header whenever
            // we switch groups, and also at the end of the iteration.
            boolean first = true;
            for (int j = 1; j < matrixInfo.numDataColumns(); j++) {
              if (delegate.displayPColumns() || !matrixInfo.isPValueColumn(j)) {
                Group nextGroup = matrixInfo.columnGroup(j);
                if (group != nextGroup) {
                  String borderStyle = first ? "darkBorderLeft" : "whiteBorderLeft";
                  String groupStyle = group.getStyleName() + "-background";
                  buildGroupHeader(rowBuilder, group, groupColumnCount,
                      borderStyle + " " + groupStyle);
                  first = false;
                  groupColumnCount = 0;
                }
                groupColumnCount++;
                group = nextGroup;
              }
            }
            String groupStyle = group.getStyleName() + "-background";
            buildGroupHeader(rowBuilder, group, groupColumnCount,
                "whiteBorderLeft " + groupStyle);
          } else {
            buildBlankHeader(rowBuilder, numSectionColumns);
          }
        }
      }
    }
    return super.buildHeaderOrFooterImpl();
  }
}
