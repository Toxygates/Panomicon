package otgviewer.server

import scala.reflect.ClassTag

import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.apache.commons.math3.stat.inference.TTest

import friedrich.data.immutable._
import t.common.shared.sample.ExpressionRow
import t.common.shared.sample.ExpressionValue

object ExprMatrix {
  val ttest = new TTest()
  val utest = new MannWhitneyUTest()

  def safeCountColumns(rows: Seq[Seq[Any]]) = 
    if (rows.size > 0) { rows(0).size } else 0
  
  def withRows(data: Seq[EVArray], metadata: ExprMatrix = null) = {
    if (metadata != null) {
      metadata.copyWith(data)
    } else {
      val rows = data.size
      val columns = safeCountColumns(data)
      new ExprMatrix(data, rows, columns, Map(), Map(), emptyAnnotations(rows))
    }       
  }
  
  def withRows(data: Seq[EVArray], rowNames: Seq[String], colNames: Seq[String]) = 
    new ExprMatrix(data, data.size, safeCountColumns(data), 
        Map() ++ rowNames.zipWithIndex, Map() ++ colNames.zipWithIndex, 
        emptyAnnotations(data.size))
  
  
  def emptyAnnotations(rows: Int) = Vector.fill(rows)(new RowAnnotation(null, List()))
}

case class RowAnnotation(probe: String, atomics: Iterable[String])
 
/**
 * Data is row-major
 * 
 * TODO: need to optimise/simplify the ExpressionValue/ExprValue/ExpressionRow classes.
 * Probably, at least 1 of these can be removed.
 * Scalability for larger matrices is a concern.
 * 
 * TODO: reconsider whether annotations are needed. Might be a major efficiency 
 * problem + redundant.
 */
class ExprMatrix(data: Seq[EVArray], rows: Int, columns: Int, 
    rowMap: Map[String, Int], columnMap: Map[String, Int], 
    val annotations: Seq[RowAnnotation]) 
    extends
    AllocatedDataMatrix[ExprMatrix, ExpressionValue, EVArray, String, String](data, rows, columns, rowMap, columnMap) {
    
  import ExprMatrix._
  import t.util.SafeMath._

  implicit def builder() = EVABuilder
  
  println(rows + " x " + columns)  
//  println(sortedColumnMap)
  
  val emptyVal = new ExpressionValue(0, 'A')
  
  /**
   * This is the bottom level copyWith method - all the other ones ultimately delegate to this one.
   */
  def copyWith(rowData: Seq[EVArray], rowMap: Map[String, Int], 
      columnMap: Map[String, Int], 
      annotations: Seq[RowAnnotation]): ExprMatrix =  {
      
        new ExprMatrix(rowData, rowData.size, 
            safeCountColumns(rowData),           
            rowMap, columnMap, annotations)
  }
  
  def copyWith(rowData: Seq[EVArray], rowMap: Map[String, Int], 
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
    new ExpressionRow(ann.probe, ann.atomics.toArray, null, null, null, x._1.toArray)
  })

  override def selectRows(rows: Seq[Int]): ExprMatrix = 
    super.selectRows(rows).copyWithAnnotations(rows.map(annotations(_)))

  /**
   * Append a two column test, which is based on the data in "sourceData".
   * sourceData must have the same number of rows as this matrix.
   */
  def appendTwoColTest(sourceData: ExprMatrix, group1: Seq[String], group2: Seq[String], 
      test: (Seq[Double], Seq[Double]) => Double, minValues: Int, colName: String): ExprMatrix = {
    val sourceCols1 = sourceData.selectNamedColumns(group1)
    val sourceCols2 = sourceData.selectNamedColumns(group2)
    
    val ps = sourceCols1.toRowVectors.zip(sourceCols2.toRowVectors) zipWithIndex
    val pvals = ps.map(r => {      
      val probe = sourceData.rowAt(r._2)
      val vs1 = r._1._1.filter(_.getPresent).map(_.getValue())
      val vs2 = r._1._2.filter(_.getPresent).map(_.getValue())
      
      if (vs1.size >= minValues && vs2.size >= minValues) {
        new ExpressionValue(test(vs1, vs2), 'P')
      } else {
        new ExpressionValue(Double.NaN, 'A')
      }
    })
    appendColumn(EVArray(pvals), colName)
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

}