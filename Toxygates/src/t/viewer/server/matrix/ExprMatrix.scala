/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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

package t.viewer.server.matrix

import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.apache.commons.math3.stat.inference.TTest
import friedrich.data.immutable._
import t.common.shared.sample.ExpressionRow
import t.db.BasicExprValue
import t.db.ExprValue
import t.viewer.server.Conversions

object ExprMatrix {
  val ttest = new TTest()
  val utest = new MannWhitneyUTest()

  def safeCountColumns(rows: Seq[Seq[Any]]) =
    if (rows.size > 0) { rows(0).size } else 0

  def withRows(data: Seq[Seq[ExprValue]], metadata: ExprMatrix = null) = {
    if (metadata != null) {
      metadata.copyWith(data)
    } else {
      val rows = data.size
      val columns = safeCountColumns(data)
      new ExprMatrix(data, rows, columns, Map(), Map(), emptyAnnotations(rows))
    }
  }

  def withRows(data: Seq[Seq[ExprValue]], rowNames: Seq[String], colNames: Seq[String]) =
    new ExprMatrix(data, data.size, safeCountColumns(data),
        Map() ++ rowNames.zipWithIndex, Map() ++ colNames.zipWithIndex,
        emptyAnnotations(data.size))

  val emptyAnnotation = new SimpleAnnotation(null)
  def emptyAnnotations(rows: Int) = Vector.fill(rows)(emptyAnnotation)
}

sealed trait RowAnnotation {
  def probe: String
  def atomics: Iterable[String]
}

/*
 * Note: it's not clear that we benefit from having multiple varieties of RowAnnotation
 */
case class FullAnnotation(probe: String, atomics: Iterable[String]) extends RowAnnotation
case class SimpleAnnotation(probe: String) extends RowAnnotation {
  def atomics = List(probe)
}

/**
 * Data is row-major
 *
 * TODO: need to optimise/simplify the ExpressionValue/ExprValue/ExpressionRow classes.
 * Probably, at least 1 of these can be removed.
 */
class ExprMatrix(data: Seq[Seq[ExprValue]], rows: Int, columns: Int,
    rowMap: Map[String, Int], columnMap: Map[String, Int],
    val annotations: Seq[RowAnnotation])
    extends
    AllocatedDataMatrix[ExprMatrix, ExprValue,
      Seq[ExprValue], String, String](data, rows, columns, rowMap, columnMap) {

  import ExprMatrix._
  import t.util.SafeMath._

  println(this)

  override def toString:String = s"ExprMatrix $rows x $columns"

  def fromSeq(s: Seq[ExprValue]) = s

  /**
   * This is the bottom level copyWith method - all the other ones ultimately delegate to this one.
   */
  def copyWith(rowData: Seq[Seq[ExprValue]], rowMap: Map[String, Int],
      columnMap: Map[String, Int],
      annotations: Seq[RowAnnotation]): ExprMatrix =  {

        new ExprMatrix(rowData, rowData.size,
            safeCountColumns(rowData),
            rowMap, columnMap, annotations)
  }

  def copyWith(rowData: Seq[Seq[ExprValue]], rowMap: Map[String, Int],
      columnMap: Map[String, Int]): ExprMatrix = {
    copyWith(rowData, rowMap, columnMap, annotations)
  }

  def copyWithAnnotations(annots: Seq[RowAnnotation]): ExprMatrix = {
    copyWith(data, rowMap, columnMap, annots)
  }

  lazy val sortedRowMap = rowMap.toSeq.sortWith(_._2 < _._2)
  lazy val sortedColumnMap = columnMap.toSeq.sortWith(_._2 < _._2)

  lazy val asRows: Seq[ExpressionRow] = toRowVectors.zip(annotations).map(x => {
    val ann = x._2
    new ExpressionRow(ann.probe, ann.atomics.toArray, null, null, null,
        x._1.map(Conversions.asJava).toArray)
  })

  override def selectRows(rows: Seq[Int]): ExprMatrix =
    super.selectRows(rows).copyWithAnnotations(rows.map(annotations(_)))

  def selectRowsFromAtomics(atomics: Seq[String]): ExprMatrix = {
    val useProbes = atomics.toSet
    val is = for (
      (r, i) <- asRows.zipWithIndex;
      ats = r.getAtomicProbes.toSet;
      if (!useProbes.intersect(ats).isEmpty)
    ) yield i
    selectRows(is)
  }

  /**
   * Append a two column test, which is based on the data in "sourceData".
   * sourceData must have the same number of rows as this matrix.
   */
  def appendTwoColTest(sourceData: ExprMatrix, group1: Seq[String], group2: Seq[String],
      test: (Seq[Double], Seq[Double]) => Double, minValues: Int, colName: String): ExprMatrix = {
    val sourceCols1 = sourceData.selectNamedColumns(group1)
    val sourceCols2 = sourceData.selectNamedColumns(group2)

    val ps = sourceCols1.toRowVectors.zip(sourceCols2.toRowVectors).zipWithIndex
    val pvals = ps.map(r => {
      val probe = sourceData.rowAt(r._2)
      val vs1 = r._1._1.filter(_.present).map(_.value)
      val vs2 = r._1._2.filter(_.present).map(_.value)

      if (vs1.size >= minValues && vs2.size >= minValues) {
        new BasicExprValue(test(vs1, vs2), 'P')
      } else {
        new BasicExprValue(Double.NaN, 'A')
      }
    })
    appendColumn(pvals, colName)
  }

  private def equals0(x: Double) = java.lang.Double.compare(x, 0d) == 0

  def appendTTest(sourceData: ExprMatrix, group1: Seq[String], group2: Seq[String],
    colName: String): ExprMatrix =
    appendTwoColTest(sourceData, group1, group2,
        (x,y) => ttest.tTest(x.toArray, y.toArray), 2, colName)

  def appendUTest(sourceData: ExprMatrix, group1: Seq[String], group2: Seq[String],
    colName: String): ExprMatrix =
    appendTwoColTest(sourceData, group1, group2,
        (x,y) => utest.mannWhitneyUTest(x.toArray, y.toArray), 2, colName)

  def appendDiffTest(sourceData: ExprMatrix, group1: Seq[String], group2: Seq[String],
    colName: String): ExprMatrix = {
    def diffTest(a1: Seq[Double], a2: Seq[Double]): Double = safeMean(a1) - safeMean(a2)

    appendTwoColTest(sourceData, group1, group2, diffTest(_, _), 1, colName)
  }
  
  def appendStatic(data: Seq[Double], name: String): ExprMatrix = {
    val vs = data.map(x => new BasicExprValue(x, 'P'))
    appendColumn(vs, name)
  }

}
