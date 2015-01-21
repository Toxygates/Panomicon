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
  
  //Warning, sampleClass is null
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
    
	def main(args: Array[String]) {
		val url = args(0)        
		val schema = new OTGSchema()
		val offset = args(1).toInt
    val limit = args(2).toInt
    
    val groups = new JList[Group]()
    for (g <- args.drop(3).map(extractGroup)) {
      groups.add(g)
    }
    
		println(s"Create instance for $url")
		val matServiceAsync = SyncProxy.newProxyInstance(classOf[MatrixService],
		    url, "matrix").asInstanceOf[MatrixService]

		val probes = Array[String]() //empty -> load all
		val vtype = ValueType.Folds
    
		val synthCols = new JList[Synthetic]()
		println("Load dataset")
		val colInfo = matServiceAsync.loadDataset(groups, probes, vtype, synthCols)
		val colNames = (0 until colInfo.numColumns()).map(colInfo.columnName(_))
    
		val amt = if (limit > 100) 100 else limit
		println(s"Get items offset $offset limit $limit")
    //sort by first column, descending
		val items = matServiceAsync.datasetItems(offset, amt, 0, false)
    
    //Column headers
    println("%15s".format("probe") +"\t%20s\t".format("gene symbols") ++ 
        colNames.mkString("\t"))
        
		for (i <- items) {      
		  print("%15s".format(i.getProbe()) + "\t" + 
          "%20s".format(i.getGeneSyms().mkString(",")) + "\t")      
		  val formatted = (0 until colInfo.numColumns()).map(j => formatVal(i.getValue(j)))
      println(formatted.mkString("\t"))
		}
	}
}