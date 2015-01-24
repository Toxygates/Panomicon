package otg

import com.gdevelop.gwt.syncrpc.SyncProxy
import otgviewer.client.rpc.MatrixService
import otgviewer.shared.Group
import otgviewer.shared.OTGSchema
import otgviewer.shared.OTGSample
import otgviewer.shared.ValueType
import otgviewer.shared.Synthetic
import t.common.shared.SampleClass
import t.common.shared.sample.ExpressionValue
import java.net.CookieManager
import java.net.CookiePolicy

object GetMatrix {
  
  import java.util.{LinkedList => JList}
  import scala.collection.JavaConversions._
  
  //Warning, controlGroup is null
  def makeSample(code: String) = 
    new OTGSample(code, new SampleClass(), null)
  
  val schema = new OTGSchema()
  
  def extractGroup(arg: String): Group = {    
     val s = arg.split("=")
     if (s.length == 1) {
       val sample = makeSample(s(0))
       new Group(schema, s(0), Array(sample))
     } else {
       val n = s(0)
       val ids = s(1).split(",")
       new Group(schema, n, ids.map(makeSample))
     }    
  }
  
  def formatVal(x: ExpressionValue): String = 
    "%.3f".format(x.getValue) + "(" + x.getCall + ")"
    
  def showHelp() {
    println("Usage: getMatrix (URL) (type) (range) group1 group2 ...")
    println("Type can be f or a.")
    println("Group definitions can of the form name=id1,id2,id3 or simply id1.")
    println("The range can be e.g. 10:60 for rows 10-60, or 'full' for the full matrix.")
  }
  
  sealed trait Range
  case class Limited(offset: Int, length: Int) extends Range
  case object Full extends Range

	def main(args: Array[String]) {
		val url = args(0)        
    
    if (args.length < 4) {
      showHelp()
      System.exit(1)
    }
    
		val schema = new OTGSchema()
    val vtype = args(1) match {
      case "f" => ValueType.Folds
      case "a" => ValueType.Absolute
      case _ => println("Unexpected value type: " + args(1) + ". " +
          "Valid types are 'f' and 'a' (fold and absolute).")
          throw new Exception("Illegal argument")
    }
    
    val range = args(2) match {
      case "full" => Full
      case _ =>
        val spl = args(2).split(":")
        if (spl.size < 2) {
          showHelp()
          throw new Exception("Invalid range specification")
        } else {
          Limited(spl(0).toInt, spl(1).toInt)
        }
    }
    
    val groups = new JList[Group]()
    for (g <- args.drop(3).map(extractGroup)) {
      groups.add(g)
    }
    
		println(s"Create instance for $url")
		val matServiceAsync = SyncProxy.newProxyInstance(classOf[MatrixService],
		    url, "matrix").asInstanceOf[MatrixService]

		val probes = Array[String]() //empty -> load all
		
		val synthCols = new JList[Synthetic]()
		println("Load dataset")
		
    val (colInfo, items) = range match {
      case Full =>
        val r = matServiceAsync.getFullData(groups, Array(), false, false, vtype)
        (r.managedMatrixInfo(), r.rows())
      case Limited(offset, limit) =>
        val amt = if (limit > 100) 100 else limit
        val colInfo = matServiceAsync.loadDataset(groups, probes, vtype, synthCols)
        println(s"Get items offset $offset limit $limit")
        //sort by first column, descending
        val rows = matServiceAsync.datasetItems(offset, amt, 0, false)    
        (colInfo, rows)
    }
    val colNames = (0 until colInfo.numColumns()).map(colInfo.columnName(_))
  
    //Column headers
    println("%15s".format("probe") +"\t%20s\t".format("entrez") ++ 
        colNames.mkString("\t"))
        
		for (i <- items) {      
		  print("%15s".format(i.getProbe()) + "\t" + 
          "%20s".format(i.getGeneIds().mkString(",")) + "\t")      
		  val formatted = (0 until colInfo.numColumns()).map(j => formatVal(i.getValue(j)))
      println(formatted.mkString("\t"))
		}
	}
}