package otgviewer.server

import friedrich.data.immutable._
import otgviewer.shared.ExpressionRow
import bioweb.shared.array.ExpressionValue
import scala.reflect.ClassTag
import friedrich.data.mutable._
import org.apache.commons.math3.stat.inference.TTest
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import scala.collection.immutable.{Vector => SVector}

object ExprMatrix extends DataMatrixBuilder {
  val ttest = new TTest()
  val utest = new MannWhitneyUTest()

//  def withRows(data: Seq[Seq[ExpressionValue]], metadata: ExprMatrix = null) = {
//    if (data.size > 0) {
//      val r = new ExprMatrix(data.size, data.head.size, metadata)
//      populate(r, (y, x) => data(y)(x))
//      r
//    } else {
//      new ExprMatrix(0, 0)
//    }
//    
//  }
//
//  def withColumns(data: Seq[ArrayVector[ExpressionValue]], metadata: ExprMatrix = null) = {
//    if (data.size > 0) {
//      val r = new ExprMatrix(data.head.size, data.size, metadata)
//      populate(r, (y, x) => data(x)(y))
//      r
//    } else {
//      new ExprMatrix(0, 0, metadata)
//    }
//    
//  }

 
}

case class RowAnnotation(probe: String, title: String, geneIds: Array[String], geneSyms: Array[String])
 
class ExprMatrix(data: Seq[VVector[ExpressionValue]], val rows: Int, val columns: Int, 
    rowMap: Map[String, Int], columnMap: Map[String, Int], 
    val annotations: SVector[RowAnnotation] = Vector.fill(rows)(new RowAnnotation(null, null, null, null)))
    extends
AllocatedDataMatrix[ExprMatrix, ExpressionValue, String, String](data, rows, columns, rowMap, columnMap) {

  import Conversions._

  val emptyVal = new ExpressionValue(0, 'A')
  
  def copyWith(rows: Seq[VVector[ExpressionValue]], rowMap: Map[String, Int], columnMap: Map[String, Int], 
      annotations: SVector[RowAnnotation]): ExprMatrix = 
        new ExprMatrix(data, data.size, data(0).size, rowMap, columnMap, annotations)
  
  def copyWith(rows: Seq[VVector[ExpressionValue]], rowMap: Map[String, Int], columnMap: Map[String, Int]): ExprMatrix =
    copyWith(rows, rowMap, columnMap, annotations)
    
  def copyWithAnnotations(annnots: SVector[RowAnnotation]): ExprMatrix = copyWith(data, rowMap, columnMap, annots)
  
  lazy val sortedRowMap = rowMap.toSeq.sortWith(_._2 < _._2)
  lazy val sortedColumnMap = columnMap.toSeq.sortWith(_._2 < _._2)
  
  lazy val asRows: SVector[ExpressionRow] = toRowVectors.toVector.zip(annotations).map(x => {
    val ann = x._2    
    new ExpressionRow(ann.probe, ann.title, ann.geneIds, ann.geneSyms, x._1.toArray)
  })

  override def selectRows(rows: Seq[Int]): ExprMatrix = 
    super.selectRows(rows).copyWithAnnotations(rows.map(annotations(_)))
  

  def appendTwoColTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String], 
      test: (Array[Double], Array[Double]) => Double): ExprMatrix = {
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
    appendColumn(ps.toSeq)    
  }
  
  def appendTTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String]): ExprMatrix = 
		  appendTwoColTest(sourceData, group1, group2, ttest.tTest(_, _))
  
  def appendUTest(sourceData: ExprMatrix, group1: Iterable[String], group2: Iterable[String]): ExprMatrix = 
  	appendTwoColTest(sourceData, group1, group2, utest.mannWhitneyUTest(_, _))
  

}