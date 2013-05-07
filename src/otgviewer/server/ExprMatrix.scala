package otgviewer.server

import friedrich.data.immutable._
import bioweb.shared.array.ExpressionRow
import bioweb.shared.array.ExpressionValue
import scala.reflect.ClassTag
import org.apache.commons.math3.stat.inference.TTest
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import scala.collection.immutable.{Vector => SVector}

object ExprMatrix {
  val ttest = new TTest()
  val utest = new MannWhitneyUTest()

  def withRows(data: Seq[Seq[ExpressionValue]], metadata: ExprMatrix = null) = {
    if (metadata != null) {
      metadata.copyWith(data)
    } else {
      val rows = data.size
      val columns = data(0).size
      new ExprMatrix(data.map(new VVector(_)), rows, columns, Map(), Map(), emptyAnnotations(rows))
    }       
  }
  
  def emptyAnnotations(rows: Int) = Vector.fill(rows)(new RowAnnotation(null, null, null, null))
}

case class RowAnnotation(probe: String, title: String, geneIds: Array[String], geneSyms: Array[String])
 
/**
 * Data is row-major
 */
class ExprMatrix(data: Seq[VVector[ExpressionValue]], rows: Int, columns: Int, 
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
  def copyWith(rows: Seq[VVector[ExpressionValue]], rowMap: Map[String, Int], columnMap: Map[String, Int], 
      annotations: SVector[RowAnnotation]): ExprMatrix = 
        new ExprMatrix(rows, rows.size, rows(0).size, rowMap, columnMap, annotations)
  
  def copyWith(rows: Seq[Seq[ExpressionValue]], rowMap: Map[String, Int], columnMap: Map[String, Int]): ExprMatrix =
    copyWith(rows.map(new VVector(_)), rowMap, columnMap, annotations)
  
  def copyWithAnnotations(annots: SVector[RowAnnotation]): ExprMatrix = copyWith(data, rowMap, columnMap, annots)
  
  lazy val sortedRowMap = rowMap.toSeq.sortWith(_._2 < _._2)
  lazy val sortedColumnMap = columnMap.toSeq.sortWith(_._2 < _._2)
  
  lazy val asRows: SVector[ExpressionRow] = toRowVectors.toVector.zip(annotations).map(x => {
    val ann = x._2    
    new ExpressionRow(ann.probe, ann.title, ann.geneIds, ann.geneSyms, x._1.toArray)
  })

  override def selectRows(rows: Seq[Int]): ExprMatrix = 
    super.selectRows(rows).copyWithAnnotations(rows.map(annotations(_)).toVector)  

  def appendTwoColTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String], 
      test: (Array[Double], Array[Double]) => Double, colName: String): ExprMatrix = {
    val cs1 = group1.toSeq.map(sourceData.column(_))
    val cs2 = group2.toSeq.map(sourceData.column(_))
    val ps = (0 until rows).map(i => {
      val vs1 = cs1.map(_(i)).filter(_.getPresent).toArray
      val vs2 = cs2.map(_(i)).filter(_.getPresent).toArray
      if (vs1.size >= 2 && vs2.size >= 2) {
        new ExpressionValue(test(vs1.map(_.getValue), vs2.map(_.getValue)), 'P')
      } else {
        new ExpressionValue(0, 'A')
      }
    })
    appendColumn(ps.toSeq, colName)    
  }
  
  def appendTTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String], colName: String): ExprMatrix = 
		  appendTwoColTest(sourceData, group1, group2, ttest.tTest(_, _), colName)
  
  def appendUTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String], colName: String): ExprMatrix = 
  	appendTwoColTest(sourceData, group1, group2, utest.mannWhitneyUTest(_, _), colName)
  

}