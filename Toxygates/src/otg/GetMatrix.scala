/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otg

import java.io.File

import java.net.URL
import scala.sys.process._
import scala.language.postfixOps
import com.gdevelop.gwt.syncrpc.SyncProxy
import otgviewer.shared.Group
import otgviewer.shared.OTGSample
import otgviewer.shared.OTGSchema
import otgviewer.shared.Synthetic
import t.common.shared.sample.ExpressionValue
import t.viewer.client.rpc.MatrixService
import t.viewer.client.rpc.SparqlService
import t.common.shared.ValueType
import t.viewer.shared.table.SortKey

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
		
    val colInfo = matServiceAsync.loadMatrix(groups, probes, vtype, synthCols)
    
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
          matServiceAsync.matrixRows(offset, amt, new SortKey.MatrixColumn(1), true)
        } else {
          matServiceAsync.matrixRows(offset, amt, new SortKey.MatrixColumn(0), false)
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