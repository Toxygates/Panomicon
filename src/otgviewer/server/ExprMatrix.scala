package otgviewer.server

import friedrich.data.immutable._
import t.common.shared.sample.ExpressionRow
import t.common.shared.sample.ExpressionValue
import scala.reflect.ClassTag
import org.apache.commons.math3.stat.inference.TTest
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import scala.collection.immutable.{Vector => SVector}
import otgviewer.server.rpc.Conversions

object ExprMatrix {
  val ttest = new TTest()
  val utest = new MannWhitneyUTest()

  def withRows(data: Seq[Seq[ExpressionValue]], metadata: ExprMatrix = null) = {
    if (metadata != null) {
      metadata.copyWith(data)
    } else {
      val rows = data.size
      val columns = data(0).size
      new ExprMatrix(data.map(_.toVector), rows, columns, Map(), Map(), emptyAnnotations(rows))
    }       
  }
  
  def withRows(data: Seq[Seq[ExpressionValue]], rowNames: Seq[String], colNames: Seq[String]) = 
    new ExprMatrix(data.map(_.toVector), data.size, data(0).size, 
        Map() ++ rowNames.zipWithIndex, Map() ++ colNames.zipWithIndex, 
        emptyAnnotations(data.size))
  
  
  def emptyAnnotations(rows: Int) = Vector.fill(rows)(new RowAnnotation(null))
}

case class RowAnnotation(probe: String) //, title: String, geneIds: Array[String], geneSyms: Array[String])
 
/**
 * Data is row-major
 */
class ExprMatrix(data: Seq[Vector[ExpressionValue]], rows: Int, columns: Int, 
    rowMap: Map[String, Int], columnMap: Map[String, Int], 
    val annotations: SVector[RowAnnotation]) 
    extends
    AllocatedDataMatrix[ExprMatrix, ExpressionValue, String, String](data, rows, columns, rowMap, columnMap) {
  
  import Conversions._
  import ExprMatrix._

  println(rows + " x " + columns)
   
  val emptyVal = new ExpressionValue(0, 'A')
  
  /**
   * This is the bottom level copyWith method - all the other ones ultimately delegate to this one.
   */
  def copyWith(rowData: Seq[Vector[ExpressionValue]], rowMap: Map[String, Int], columnMap: Map[String, Int], 
      annotations: SVector[RowAnnotation]): ExprMatrix = 
        new ExprMatrix(rowData, rowData.size, 
            if (rowData.isEmpty) { 0 } else { rowData(0).size }, 
            rowMap, columnMap, annotations)
  
  def copyWith(rowData: Seq[Seq[ExpressionValue]], rowMap: Map[String, Int], columnMap: Map[String, Int]): ExprMatrix =
    copyWith(rowData.map(_.toVector), rowMap, columnMap, annotations)
  
  def copyWithAnnotations(annots: SVector[RowAnnotation]): ExprMatrix = copyWith(data, rowMap, columnMap, annots)
  
  lazy val sortedRowMap = rowMap.toSeq.sortWith(_._2 < _._2)
  lazy val sortedColumnMap = columnMap.toSeq.sortWith(_._2 < _._2)
  
  lazy val asRows: SVector[ExpressionRow] = toRowVectors.toVector.zip(annotations).map(x => {
    val ann = x._2    
    new ExpressionRow(ann.probe, null, null, null, x._1.toArray)
  })

  override def selectRows(rows: Seq[Int]): ExprMatrix = 
    super.selectRows(rows).copyWithAnnotations(rows.map(annotations(_)).toVector)

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
    import otg.SafeMath._
    def diffTest(a1: Seq[Double], a2: Seq[Double]): Double = safeMean(a1) - safeMean(a2)

    appendTwoColTest(sourceData, group1, group2, diffTest(_, _), 1, colName)
  }

}