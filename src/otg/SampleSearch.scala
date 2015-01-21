package otg

import com.gdevelop.gwt.syncrpc.SyncProxy
import otgviewer.client.rpc.SparqlService
import t.common.shared.SampleClass
import scala.collection.JavaConversions._
import otgviewer.shared.OTGSchema

object SampleSearch {
  def showHelp() {
    println("Usage: sampleSearch (url) param1=val1 param2=val2 ...")
    val s = new OTGSchema()
    val allParams = s.macroParameters() ++ 
      List(s.majorParameter(), s.mediumParameter(), s.minorParameter())
    println("Valid parameters include: " + allParams.mkString(" "))
  }
  
  def main(args: Array[String]) {
    if (args.length < 2) {
      showHelp()
      System.exit(1)
    }
    
    val url = args(0)
    val sparqlServiceAsync = SyncProxy.newProxyInstance(classOf[SparqlService],
        url, "sparql").asInstanceOf[SparqlService]
    
    val sc = new SampleClass()
    for (kv <- args.drop(1)) {
      val s = kv.split("=")
      val (k, v) = (s(0), s(1))
      sc.put(k, v)
    }
    println(sc)
    
    val unwantedKeys = List("id", "x")
    
    val r = sparqlServiceAsync.samples(sc)
    if (r.length == 0) {
      println("No samples found.")
    } else {
      for (s <- r) {
        val m = mapAsScalaMap(s.sampleClass().getMap)
        val filteredm = m.filter( x => ! unwantedKeys.contains(x._1))
        println(s.getCode() + "\t" + filteredm.map(x => x._1 + "=" + x._2).mkString("\t"))
      }
      println(r.map(_.getCode).mkString(" "))
    }
  }
    
}