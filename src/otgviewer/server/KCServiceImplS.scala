package otgviewer.server
import otgviewer.shared._
import otg.ExprValue

object KCServiceImplS {
  import scala.collection.JavaConversions._

  def barcodes(columns: Iterable[DataColumn]): Array[String] = {
    columns.flatMap(_ match {
      case g: Group   => g.getBarcodes
      case b: Barcode => Array(b)      
    }).map(_.getCode).toArray    
  }

  def barcodes4J(columns: java.lang.Iterable[DataColumn]): Array[String] = barcodes(columns)

  def computeColumn(col: DataColumn, rawData: Array[ExprValue], orderedBarcodes: Array[String]): ExprValue = {
    col match {
      case g: Group => {
        val bcs = g.getBarcodes
        val ids = bcs.map(x => orderedBarcodes.indexOf(x.getCode))
        val vs = ids.map(rawData(_))
        val call = (if (vs.exists(_.call != 'A')) { 'P' } else { 'A' })
        val pvals = vs.filter(_.call != 'A')
        new ExprValue(pvals.map(_.value).fold(0.0)(_+_) / pvals.size, call, rawData(0).probe)
      }
      case b: Barcode => {
        val i = orderedBarcodes.indexOf(b.getCode)
        rawData(i)
      }
    }
  }
  
  def computeRow(cols: Iterable[DataColumn], rawData: Array[Array[ExprValue]], 
      orderedBarcodes: Array[String], row: Int): Array[ExprValue] = {
    cols.map(c => {
      computeColumn(c, rawData(row), orderedBarcodes)
    }).toArray
  }
  
  def computeRow4J(cols: java.lang.Iterable[DataColumn], rawData: Array[Array[ExprValue]], 
      orderedBarcodes: Array[String], row: Int): Array[ExprValue] = computeRow(cols, rawData, orderedBarcodes, row)
  
 
}