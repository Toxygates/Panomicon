package otgviewer.server

import friedrich.statistics._
import otg.ExprValue
import otgviewer.shared.ExpressionRow
import otgviewer.shared.ExpressionValue
import scala.reflect.ClassTag
import friedrich.statistics.DataMatrixBuilder
import friedrich.statistics.ArrayMatrix
import org.apache.commons.math3.stat.inference.TTest
import org.apache.commons.math3.stat.inference.MannWhitneyUTest

object ExprMatrix extends DataMatrixBuilder {
  val ttest = new TTest()
  val utest = new MannWhitneyUTest()

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

/**
 * Future: lift up some of this functionality into the Friedrich matrix library.
 * Use immutable rather than mutable matrices.
 */
class ExprMatrix(rows: Int, columns: Int, metadata: ExprMatrix = null) extends ArrayMatrix[ExprValue](rows, columns, ExprValue(0, 'A'))
  with RowColAllocation[ExprValue, ArrayVector[ExprValue], String, String] {

  import ExprMatrix._
  import Conversions._

  var annotations: Array[RowAnnotation] = _

  if (metadata != null) {
    annotations = metadata.annotations
    rowMap = metadata.rowMap
    columnMap = metadata.columnMap
  } else {
    annotations = Array.fill(rows)(new RowAnnotation(null, null, null, null))
  }
  
  def sortedRowMap = rowMap.toSeq.sortWith(_._2 < _._2)
  def sortedColumnMap = columnMap.toSeq.sortWith(_._2 < _._2)
  
  lazy val asRows: scala.collection.immutable.Vector[ExpressionRow] = toRowVectors.toVector.zip(annotations).map(x => {
    val ann = x._2    
    new ExpressionRow(ann.probe, ann.title, ann.geneIds, ann.geneSyms,
      x._1.toArray.map(asJava(_)))
  })

  def filterRows(f: (ArrayVector[ExprValue]) => Boolean): ExprMatrix = {
    val x = toRowVectors.zip(annotations).zip(sortedRowMap.map(_._1)).filter(x => f(x._1._1))
    val r = ExprMatrix.withRows(x.map(_._1._1), this)
    r.annotations = x.map(_._1._2).toArray
    r.rowMap = Map() ++ x.map(_._2).zipWithIndex    
    r
  }

  def appendColumn(col: Seq[ExprValue]): ExprMatrix = {
    val r = new ExprMatrix(rows, columns + 1, this)
    populate(r, (y, x) => if (x < columns) { data(y)(x) } else { col(y) })    
    r
  }

  def appendTwoColTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String], 
      test: (Array[Double], Array[Double]) => Double): ExprMatrix = {
    val cs1 = group1.toSeq.map(sourceData.column(_))
    val cs2 = group2.toSeq.map(sourceData.column(_))
    val ps = (0 until rows).map(i => {
      val vs1 = cs1.map(_(i)).filter(_.call != 'A').toArray
      val vs2 = cs2.map(_(i)).filter(_.call != 'A').toArray
      if (vs1.size >= 2 && vs2.size >= 2) {
        ExprValue(test(vs1.map(_.value), vs2.map(_.value)), 'P', vs1.head.probe)
      } else {
        ExprValue(0, 'A', cs1.head(i).probe)
      }
    })
    val r = appendColumn(ps.toSeq)
    r
  }
  
  def appendTTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String]): ExprMatrix = 
		  appendTwoColTest(sourceData, group1, group2, ttest.tTest(_, _))
  
  def appendUTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String]): ExprMatrix = 
  	appendTwoColTest(sourceData, group1, group2, utest.mannWhitneyUTest(_, _))
  

  def sortRows(f: (ArrayVector[ExprValue], ArrayVector[ExprValue]) => Boolean): ExprMatrix = {
    val sortedKeys = sortedRowMap.map(_._1)
    val sort = toRowVectors.zip(sortedKeys).zip(annotations)
    val sorted = sort.sortWith((x, y) => f(x._1._1, y._1._1))
    val r = ExprMatrix.withRows(sorted.map(_._1._1), this)
    r.annotations = sorted.map(_._2).toArray
    r.rowMap = Map() ++ sorted.map(_._1._2).zipWithIndex    
    r
  }

  /**
   * Adjoin another matrix to this one on the right hand side.
   * The two matrices must have the same number of rows
   * and be sorted in the same way.
   * The metadata of the resulting matrix is taken from 'this' where appropriate.
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
   * The metadata of the resulting matrix is taken from 'this' where appropriate.
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
   * The metadata of the resulting matrix is taken from 'this' where appropriate.
   */
  def modifyJointly(other: ExprMatrix, f: (ExprMatrix) => ExprMatrix): (ExprMatrix, ExprMatrix) = {
    val joined = adjoinRight(other)
    val modified = f(joined)
    modified.verticalSplit(columns)
  }
  
  /**
   * NB this allows for permutation as well as selection
   */
  def selectRows(rows: Seq[Int]): ExprMatrix = {
    val r = ExprMatrix.withRows(rows.map(row(_)), this)    
    val rowIds = rows.toSet
    r.annotations = rows.map(a => annotations(a)).toArray
    r.rowMap = Map() ++ rows.map(rowNames(_)).zipWithIndex
    r
  }
  
  /**
   * NB this allows for permutation as well as selection
   */
  def selectNamedRows(rows: Seq[String]) = selectRows(rows.map(rowMap(_)))   
  
  /**
   * NB this allows for permutation as well as selection
   */
  def selectColumns(columns: Seq[Int]): ExprMatrix = {
    val r = ExprMatrix.withColumns(columns.map(column(_)), this)
    val colIds = columns.toSet
    r.columnMap = Map() ++ columns.map(columnNames(_)).zipWithIndex 
    r
  }
  
  /**
   * NB this allows for permutation as well as selection
   */
  def selectNamedColumns(columns: Seq[String]) = selectColumns(columns.map(columnMap(_)))
  
}