package t.viewer.client.table;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.cell.client.*;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;

import t.common.client.ImageClickCell;
import t.common.client.Resources;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.shared.AssociationValue;
import t.viewer.shared.ManagedMatrixInfo;

/**
 * Some column management code factored out of ExpressionTable.
 * 
 * Next steps: refactor ExpressionTable.setupColumns and related functions, and
 * move some of the logic here.
 */
public class ETColumns {
  private final Resources resources;
  private Delegate delegate;
  private Logger logger;

  private final String columnWidth;

  public interface Delegate {
    TableStyle style();
    void addColumn(Column<ExpressionRow, ?> col, String section, ColumnInfo info);
    void onToolCellClickedForProbe(String probe);

    Column<ExpressionRow, ?> sectionColumnAtIndex(String desiredSection, int sectionIndex);

    int columnCountForSection(String section);
  }

  public ETColumns(Delegate delegate, Resources resources, String columnWidth, Logger logger) {
    this.delegate = delegate;
    this.resources = resources;
    this.columnWidth = columnWidth;
    this.logger = logger;
  }

  public void addDataColumns(ManagedMatrixInfo matrixInfo, boolean displayPColumns) {
    TextCell tc = new TextCell();
    Group previousGroup = null;
    for (int i = 0; i < matrixInfo.numDataColumns(); ++i) {
      if (displayPColumns || !matrixInfo.isPValueColumn(i)) {
        Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, i);

        Group group = matrixInfo.columnGroup(i);
        String groupStyle = group == null ? "dataColumn" : group.getStyleName();
        String borderStyle = (group != previousGroup) ? "darkBorderLeft" : "lightBorderLeft";
        String style = groupStyle + " " + borderStyle;

        logger.info(matrixInfo.shortColumnName(i) + " " + matrixInfo.columnFilter(i).threshold + " "
            + matrixInfo.columnFilter(i).active());
        ColumnInfo columnInfo = new ColumnInfo(matrixInfo.shortColumnName(i), matrixInfo.columnHint(i), true, false,
            columnWidth, style, false, true, matrixInfo.columnFilter(i).active());
        columnInfo.setHeaderStyleNames(style);

        previousGroup = group;
        delegate.addColumn(valueCol, "data", columnInfo);
      }
    }
  }

  public ColumnSortInfo recoverSortColumn(ManagedMatrixInfo matrixInfo, ColumnSortInfo oldSortInfo) {
    Column<ExpressionRow, ?> sortColumn = null;
    if (oldSortInfo != null) {
      int oldSortIndex = ((ExpressionColumn) oldSortInfo.getColumn()).matrixColumn();

      if (oldSortIndex < matrixInfo.numDataColumns()) {
        sortColumn = delegate.sectionColumnAtIndex("data", oldSortIndex);
      } else if (oldSortIndex < matrixInfo.numColumns()) {
        sortColumn = delegate.sectionColumnAtIndex("synthetic", oldSortIndex - delegate.columnCountForSection("data"));
      }

      if (sortColumn != null) {
        return new ColumnSortInfo(sortColumn, oldSortInfo.isAscending());
      }
    }
    return null;
  }

  protected Column<ExpressionRow, String> toolColumn(Cell<String> cell) {
    return new Column<ExpressionRow, String>(cell) {
      @Override
      public String getValue(ExpressionRow er) {
        if (er != null) {
          return er.getProbe();
        } else {
          return "";
        }
      }
    };
  }

  public ToolCell toolCell() {
    return new ToolCell();
  }

  class ToolCell extends ImageClickCell.StringImageClickCell {
    public ToolCell() {
      super(resources.chart(), "charts", false);
    }

    @Override
    public void onClick(final String value) {
      delegate.onToolCellClickedForProbe(value);
    }
  }

  public void addSynthColumns(ManagedMatrixInfo matrixInfo) {
    boolean first = true;
    for (int i = matrixInfo.numDataColumns(); i < matrixInfo.numColumns(); i++) {
      String borderStyle = first ? "darkBorderLeft" : "lightBorderLeft";
      first = false;
      addSynthColumn(matrixInfo, i, borderStyle);
    }
  }

  public ExpressionColumn addSynthColumn(ManagedMatrixInfo matrixInfo, int column, String borderStyle) {
    TextCell tc = new TextCell();
    ExpressionColumn synCol = new ExpressionColumn(tc, column);

    ColumnInfo info = new ColumnInfo(matrixInfo.shortColumnName(column), matrixInfo.columnHint(column), true, false,
        columnWidth, "extraColumn " + borderStyle, false, true, matrixInfo.columnFilter(column).active());
    info.setHeaderStyleNames(borderStyle);
    info.setDefaultSortAsc(true);
    delegate.addColumn(synCol, "synthetic", info);
    return synCol;
  }

  protected Header<SafeHtml> getColumnHeader(ColumnInfo info) {
    ColumnInfo i = info.trimTitle(RichTable.COL_TITLE_MAX_LEN);
    SafeHtmlHeader superHeader = new SafeHtmlHeader(i.headerHtml());

    if (info.filterable()) {
      FilteringHeader header = new FilteringHeader(superHeader.getValue(), info.filterActive());
      header.setHeaderStyleNames(info.headerStyleNames());
      return header;
    } else {
      return superHeader;
    }
  }

  protected List<HideableColumn<ExpressionRow, ?>> createHideableColumns(DataSchema schema) {
    SafeHtmlCell htmlCell = new SafeHtmlCell();
    List<HideableColumn<ExpressionRow, ?>> columns = new ArrayList<HideableColumn<ExpressionRow, ?>>();

    columns.add(new LinkingColumn<ExpressionRow>(htmlCell, "Gene ID", StandardColumns.GeneID, delegate.style()) {
      @Override
      protected String formLink(String value) {
        return AType.formGeneLink(value);
      }

      @Override
      protected Collection<AssociationValue> getLinkableValues(ExpressionRow er) {
        String[] geneIds = er.getGeneIds(); // basis for the URL
        String[] labels = er.getGeneIdLabels();
        List<AssociationValue> r = new ArrayList<AssociationValue>();
        for (int i = 0; i < geneIds.length; i++) {
          r.add(new AssociationValue(labels[i], geneIds[i], null));
        }
        return r;
      }
    });

    columns
        .add(new HTMLHideableColumn<ExpressionRow>(htmlCell, "Gene Symbol", StandardColumns.GeneSym, delegate.style()) {
      @Override
      protected String getHtml(ExpressionRow er) {
        return mkAssociationList(er.getGeneSyms());
      }

    });

    columns.add(
        new HTMLHideableColumn<ExpressionRow>(htmlCell, "Probe Title", StandardColumns.ProbeTitle, delegate.style()) {
      @Override
      protected String getHtml(ExpressionRow er) {
        return mkAssociationList(er.getAtomicProbeTitles());
      }
    });

    columns.add(new LinkingColumn<ExpressionRow>(htmlCell, "Probe", StandardColumns.Probe, delegate.style()) {

      @Override
      protected String formLink(String value) {
        return null;
      }

      @Override
      protected Collection<AssociationValue> getLinkableValues(ExpressionRow er) {
        List<AssociationValue> r = new LinkedList<AssociationValue>();
        for (String probe : er.getAtomicProbes()) {
          r.add(new AssociationValue(probe, probe, null));
        }
        return r;
      }
    });

    return columns;
  }

  private String mkAssociationList(String[] values) {
    return SharedUtils.mkString("<div class=\"associationValue\">", values, "</div> ");
  }
}
