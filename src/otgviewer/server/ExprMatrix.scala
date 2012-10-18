package otgviewer.server

import friedrich.statistics._
import otg.ExprValue
import otgviewer.shared.ExpressionRow
import otgviewer.shared.ExpressionValue
import scala.reflect.ClassTag
import friedrich.statistics.DataMatrixBuilder
import friedrich.statistics.ArrayMatrix
import org.apache.commons.math3.stat.inference.TTest

object ExprMatrix extends DataMatrixBuilder {
  val ttest = new TTest()

  def withRows(data: Seq[Seq[ExprValue]], metadata: ExprMatrix = null) = {
    if (data.size > 0) {
      val r = new ExprMatrix(data.size, data.head.size, metadata)
      populate(r, (y, x) => data(y)(x))
      r
    } else {
      new ExprMatrix(0, 0)
    }
    
  }

  def withColumns(data: Seq[ArrayVector[ExprValue]], metadata: ExprMatrix = null) = {
    if (data.size > 0) {
      val r = new ExprMatrix(data.head.size, data.size, metadata)
      populate(r, (y, x) => data(x)(y))
      r
    } else {
      new ExprMatrix(0, 0, metadata)
    }
    
  }

  case class RowAnnotation(probe: String, title: String, geneIds: Array[String], geneSyms: Array[String])
}

class ExprMatrix(rows: Int, columns: Int, metadata: ExprMatrix = null) extends ArrayMatrix[ExprValue](rows, columns, ExprValue(0, 'A'))
  with RowColAllocation[ExprValue, ArrayVector[ExprValue], String, String] {

  import ExprMatrix._

  var annotations: Array[RowAnnotation] = Array.fill(rows)(new RowAnnotation(null, null, null, null))

  if (metadata != null) {
    annotations = metadata.annotations
    rowMap = metadata.rowMap
    columnMap = metadata.columnMap
  }
  
  def asRows: Iterable[ExpressionRow] = toRowVectors.zip(annotations).map(x => {
    val ann = x._2
    new ExpressionRow(ann.probe, ann.title, ann.geneIds, ann.geneSyms,
      x._1.toArray.map(v => new ExpressionValue(v.value, v.call)))
  })

  def filterRows(f: (ArrayVector[ExprValue]) => Boolean): ExprMatrix = {
    val x = toRowVectors.zip(annotations).zip(rowMap).filter(x => f(x._1._1))
    val r = ExprMatrix.withRows(x.map(_._1._1), this)
    r.annotations = x.map(_._1._2).toArray
    r.rowMap = Map() ++ x.map(_._2)    
    r
  }

  def appendColumn(col: Seq[ExprValue]): ExprMatrix = {
    val r = new ExprMatrix(rows, columns + 1, this)
    populate(r, (y, x) => if (x < columns) { data(y)(x) } else { col(y) })    
    r
  }

  def appendTTest(sourceData: ExprMatrix, group1: Iterable[Int], group2: Iterable[Int]): ExprMatrix = {
    val cs1 = group1.toSeq.map(sourceData.column(_))
    val cs2 = group2.toSeq.map(sourceData.column(_))
    val ps = (0 until rows).map(i => {
      val vs1 = cs1.map(_(i)).filter(_.call != 'A').toArray
      val vs2 = cs2.map(_(i)).filter(_.call != 'A').toArray
      if (vs1.size >= 2 && vs2.size >= 2) {
        ExprValue(ttest.tTest(vs1.map(_.value), vs2.map(_.value)), 'P', vs1.head.probe)
      } else {
        ExprValue(0, 'A', cs1.head(i).probe)
      }
    })
    val r = appendColumn(ps.toSeq)
    r
  }

  def sortRows(f: (ArrayVector[ExprValue], ArrayVector[ExprValue]) => Boolean): ExprMatrix = {
    val sort = toRowVectors.zip(rowMap).zip(annotations)
    val sorted = sort.sortWith((x, y) => f(x._1._1, y._1._1))
    val r = ExprMatrix.withRows(sorted.map(_._1._1), this)
    r.annotations = sorted.map(_._2).toArray
    r.rowMap = Map() ++ sorted.map(_._1._2)    
    r
  }

  /**
   * Adjoin another matrix to this one on the right hand side.
   * The two matrices must have the same number of rows
   * and be sorted in the same way.
   */
  def adjoinRight(other: ExprMatrix): ExprMatrix = {
    val r = new ExprMatrix(rows, columns + other.columns, this)
    populate(r, (y, x) => if (x < columns) { data(y)(x) } else { other.data(y)(x - columns) })    
    r.columnMap = columnMap ++ other.columnMap.map(x => (x._1, x._2 + columns))
    r
  }

  /**
   * Split this matrix vertically so that the first column in the second matrix
   * has the given offset in this matrix.
   */
  def verticalSplit(at: Int): (ExprMatrix, ExprMatrix) = {
    val r1 = new ExprMatrix(rows, at, this)
    val r2 = new ExprMatrix(rows, columns - at, this)
    populate(r1, (y, x) => data(y)(x))
    populate(r2, (y, x) => data(y)(x + at))
    
    r1.columnMap = columnMap.filter(_._2 < at)
    r2.columnMap = columnMap.filter(_._2 >= at).map(x => (x._1, x._2 - at))
    (r1, r2)
  }
  
  /**
   * Adjoin two matrices, modify them together, then split again.
   */
  def modifyJointly(other: ExprMatrix, f: (ExprMatrix) => ExprMatrix): (ExprMatrix, ExprMatrix) = {
    val joined = adjoinRight(other)
    val modified = f(joined)
    modified.verticalSplit(columns)
  }

}