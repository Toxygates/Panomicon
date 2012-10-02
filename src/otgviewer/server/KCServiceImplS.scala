package otgviewer.server
import otgviewer.shared._
import otg.ExprValue
import org.apache.commons.math3.stat.inference.TTest

object KCServiceImplS {
  import scala.collection.JavaConversions._
  
  val ttest = new TTest()

  def barcodes(columns: Iterable[DataColumn]): Array[String] = {
    columns.flatMap(_ match {
      case g: Group   => g.getBarcodes
      case b: Barcode => Vector(b)      
      case _ => Vector()
    }).map(_.getCode).toArray    
  }

  def barcodes4J(columns: java.lang.Iterable[DataColumn]): Array[String] = barcodes(columns)
  def barcodes4J(columns: Array[DataColumn]): Array[String] = barcodes(columns.toIterable)
  
  def computeColumn(col: DataColumn, rawData: Array[ExprValue], colMap: Map[String, Int]): ExprValue = {
    col match {
      case g: Group => {
        val bcs = g.getBarcodes
        val ids = bcs.map(b => colMap(b.getCode))
        val vs = ids.map(rawData(_))
        val call = (if (vs.exists(_.call != 'A')) { 'P' } else { 'A' })
        val pvals = vs.filter(_.call != 'A')
        new ExprValue(pvals.map(_.value).sum / pvals.size, call, rawData(0).probe)
      }
      case b: Barcode => {        
        rawData(colMap(b.getCode))
      }
      case _ => {
        throw new Exception("Unexpected column type")
      }
    }
  }
  
  /**
   * Perform a T-test on the two specified columns.
   * The result will be the renderInto row with the t-test p-value added as a final item at the end.
   */
  def performTTest(col1: Group, col2: Group, row: Array[ExprValue], renderInto: Array[ExprValue], colMap: Map[String, Int]): Array[ExprValue] = {
    val bcs1 = col1.getBarcodes
    val vs1 = bcs1.map(b => colMap(b.getCode)).map(row(_))
    val bcs2 = col2.getBarcodes
    val vs2 = bcs2.map(b => colMap(b.getCode)).map(row(_))
    
    val fvs1 = vs1.filter(_.call != 'A')
    val fvs2 = vs2.filter(_.call != 'A')
    val p = (if (fvs1.size >= 2 && fvs2.size >= 2) {
      ExprValue(ttest.tTest(fvs1.map(_.value), fvs2.map(_.value)), 'P', vs1.head.probe)
    } else {
      ExprValue(0, 'A', vs1.head.probe)
    })
     
    (renderInto.toVector :+ p).toArray    
  }
  
  private def makeColMap(orderedBarcodes: Array[String]): Map[String, Int] = Map() ++ orderedBarcodes.zipWithIndex.map(x => (x._1, x._2))
  
  def computeRow(cols: Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]], 
      orderedBarcodes: Array[String], 
      row: Int): Array[ExprValue] = {
    cols.map(computeColumn(_, rawData(row), makeColMap(orderedBarcodes))).toArray
  }
  
  def computeRow4J(cols: java.lang.Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]], 
      orderedBarcodes: Array[String],
      row: Int): Array[ExprValue] = computeRow(cols, rawData, orderedBarcodes, row)
  
 
  def computeRows(cols: Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]],
      orderedBarcodes: Array[String]): Array[Array[ExprValue]] = {
    val colmap = makeColMap(orderedBarcodes)
    (0 until rawData.size).par.map(r => cols.map(computeColumn(_, rawData(r), colmap)).toArray).seq.toArray        
  }
  
  def performTTests(col1: Group, col2: Group, data: Array[Array[ExprValue]],
      renderInto: Array[Array[ExprValue]], orderedBarcodes: Array[String]): Array[Array[ExprValue]] = {
    val colmap = makeColMap(orderedBarcodes)
    (0 until data.size).par.map(r => performTTest(col1, col2, data(r), renderInto(r), colmap)).seq.toArray
  }
  
  def computeRows4J(cols: java.lang.Iterable[DataColumn], 
      rawData: Array[Array[ExprValue]],
      orderedBarcodes: Array[String]): Array[Array[ExprValue]] = computeRows(cols, rawData, orderedBarcodes)
      
  /**
   * Sort array 1 and 2 simultaneously according to a column in a1.
   */
  def sortData4J(a1: Array[Array[ExprValue]], sortCol: Int, asc: Boolean, a2: Array[Array[ExprValue]]): Array[Array[Array[ExprValue]]] = {
    val z = a1.zip(a2)
    val s = z.sortWith((p1, p2) => {
      val ev1 = p1._1(sortCol)
      val ev2 = p2._1(sortCol)
      val ascFactor = if (asc) { 1 } else { -1 }
      if (ev1.call == 'A' && ev2.call != 'A') {
        false
      } else if (ev1.call != 'A' && ev2.call == 'A') {
        true
      } else {
        if (asc) { ev1.value < ev2.value } else { ev1.value >= ev2.value }
      }
    })
    val (u1, u2) = s.unzip
    Array(u1.toArray, u2.toArray)
  }
  
}