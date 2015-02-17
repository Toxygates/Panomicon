package otg

import com.gdevelop.gwt.syncrpc.SyncProxy
import otgviewer.shared.Group
import otgviewer.shared.OTGSchema
import otgviewer.shared.OTGSample
import otgviewer.shared.ValueType
import otgviewer.shared.Synthetic
import t.common.shared.SampleClass
import t.common.shared.sample.ExpressionValue
import t.common.client.rpc.MatrixService
import java.net.CookieManager
import java.net.CookiePolicy
import sys.process._
import java.net.URL
import java.io.File
import otgviewer.client.rpc.SparqlService

object GetMatrix {
  
  import java.util.{LinkedList => JList}
  import scala.collection.JavaConversions._
  
  val schema = new OTGSchema()
  
  def extractGroup(arg: String, ss: Map[String, OTGSample]): Group = {    
     val s = arg.split("=")
     if (s.length == 1) {
       val sample = ss(s(0))
       new Group(schema, s(0), Array(sample))
     } else {
       val n = s(0)
       val ids = s(1).split(",")
       val samples = ids.map(ss(_))
       if (samples.exists(_.get("dose_level") == "Control") &&
           samples.exists(_.get("dose_level") != "Control")) {
         println(s"The group $n contains mixed control and non-control samples.")
         println("Please request them in separate groups.")
         throw new Exception("Mixed control and non-control samples")
       }
       new Group(schema, n, ids.map(ss(_)))       
     }    
  }
  
  def discoverSamples(arg: String): List[String] = {
     val s = arg.split("=")
     if (s.length == 1) {
       List(s(0))       
     } else {
       val n = s(0)
       s(1).split(",").toList       
     }    
  }
  
  
  def formatSci(x: ExpressionValue): String =
    "%.3e".format(x.getValue)
    
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
    
    println(s"Create instance for $url")
    val sServiceAsync = SyncProxy.newProxyInstance(classOf[SparqlService],
    		url, "sparql").asInstanceOf[SparqlService]

    val samples = args.drop(3).flatMap(discoverSamples)
    val resolvedSamples = Map() ++ sServiceAsync.samplesById(samples).map(x => x.getCode -> x)
    
    val groups = new JList[Group]()
    for (g <- args.drop(3).map(x => extractGroup(x, resolvedSamples))) {
      groups.add(g)
    }

		val matServiceAsync = SyncProxy.newProxyInstance(classOf[MatrixService],
		    url, "matrix").asInstanceOf[MatrixService]

		val probes = Array[String]() //empty -> load all
		
		val synthCols = new JList[Synthetic]()
		println("Load dataset")
		
    val colInfo = matServiceAsync.loadDataset(groups, probes, vtype, synthCols)
    
    range match {
      case Full =>        
        val url = matServiceAsync.prepareCSVDownload(false)
        val target = url.split("/").last
        println(s"Downloading $url to $target")
        new URL(url) #> new File(target) !!
      case Limited(offset, limit) =>
        val amt = if ((limit - offset) > 100) 100 else (limit - offset)
        
        println(s"Get items offset $offset limit $limit")
        
        val specialPSort = (colInfo.numColumns() > 1 && colInfo.columnName(1).endsWith("(p)"))

        //sort by first column, descending
        val items = if (specialPSort) {
          println("Using p-value sort on column " + colInfo.columnName(1))
          matServiceAsync.datasetItems(offset, amt, 1, true)
        } else {
          matServiceAsync.datasetItems(offset, amt, 0, false)
        }
        
        val colNames = (0 until colInfo.numColumns()).map(colInfo.columnName(_))
  
        //Column headers
        println("%15s".format("probe") +"\t%20s\t".format("entrez") ++ 
        		colNames.mkString("\t"))

        for (i <- items) {      
        	print("%15s".format(i.getProbe()) + "\t" + 
        			"%20s".format(i.getGeneIds().mkString(",")) + "\t")      
        	val formatted = (0 until colInfo.numColumns()).map(j => {
            //TODO this is fragile
            val isP = colInfo.columnName(j).endsWith("(p)")            
            if (isP) formatSci(i.getValue(j)) else formatVal(i.getValue(j))
          })
        	println(formatted.mkString("\t"))
        }
		}
	}
}