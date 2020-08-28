package t.viewer.server.matrix

import t.Context
import t.common.shared.sample
import t.viewer.server.Conversions

/**
 * Annotates data from a ManagedMatrix with additional information
 * to build full expression rows.
 * Two kinds of results can be obtained: the Scala ExpressionRow,
 * and the GWT-compatible sample.ExpressionRow.
 *
 * @param context
 * @param control
 */
class MatrixPages(context: Context, control: MatrixController) {
  import MatrixPages._
  import t.common.server.GWTUtils._


  /**
   * Get an annotated page. Changes the state of the matrix if it is a network.
   * @param offset
   * @param length
   * @return
   */
  def getPageView(offset: Int, length: Int, symbols: Boolean): Seq[ExpressionRow] = {
    val view = control.managedMatrix.getPageView(offset, length)
    control.insertAnnotations(context, view, symbols)
  }

  /**
   * Get an annotated page as GWT rows. Changes the state of the matrix if it is a network.
   * @param offset
   * @param length
   * @return
   */
  def getPageViewGWT(offset: Int, length: Int, tooltips: Boolean, symbols: Boolean): GWTList[sample.ExpressionRow] = {
    val view = getPageView(offset, length, symbols)
    val rows = asGWT(view)
    if (tooltips) { setTooltips(rows) }
    rows.asGWT
  }

  def setTooltips(grouped: Seq[sample.ExpressionRow]): Unit = {
    val rowNames = grouped.map(_.getProbe)
    val mm = control.managedMatrix
    val rawData = mm.rawUngrouped.selectNamedRows(rowNames).rowData

    for {
      (gr, rr) <- grouped zip rawData;
      (gv, i) <- gr.getValues.zipWithIndex
    } {
      val tooltip = if (mm.info.isPValueColumn(i)) {
        "p-value (t-test treated against control)"
      } else {
        val basis = mm.baseColumns(i)
        val rawRow = basis.map(i => rr(i))
        ManagedMatrix.makeTooltip(rawRow)
      }
      gv.setTooltip(tooltip)
    }
  }

}

object MatrixPages {
  def asGWT(rows: Seq[ExpressionRow]): Seq[sample.ExpressionRow] = {
    rows.map(r => {
      val nr = new sample.ExpressionRow(r.probe, r.atomicProbes, r.probeTitles, r.geneIds, r.geneSymbols,
        r.values.map(Conversions.asJava))
      nr.setGeneIdLabels(r.geneIdLabels)
      nr
    })
  }
}