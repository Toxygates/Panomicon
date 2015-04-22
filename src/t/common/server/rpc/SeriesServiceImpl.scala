package t.common.server.rpc

import java.util.ArrayList
import java.util.{List => JList}
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asJavaCollection
import otgviewer.server.rpc.Conversions
import otgviewer.server.rpc.Conversions.asJava
import otgviewer.server.rpc.Conversions.asScala
import otgviewer.shared.MatchResult
import otgviewer.shared.NoSuchProbeException
import otgviewer.shared.RankRule
import otgviewer.shared.{Series => SSeries}
import t.BaseConfig
import t.common.client.rpc.SeriesService
import t.common.shared.SampleClass
import t.db.MatrixContext
import t.db.SeriesDB
import t.db.Series
import t.sparql.Probes
import t.SeriesRanking
import t.viewer.server.Conversions._
import t.common.shared.Dataset
import t.sparql.Datasets

abstract class SeriesServiceImpl[S <: Series[S]] extends TServiceServlet with SeriesService {
  import scala.collection.JavaConversions._  

  import java.lang.{ Double => JDouble }
 
  private implicit def mcontext: MatrixContext = context.matrix
  implicit protected def context: t.Context

  protected def getDB(): SeriesDB[S]
  
  protected def ranking(db: SeriesDB[S], key: S): SeriesRanking[S] = new SeriesRanking(db, key) 
  
  implicit protected def asShared(s: S): SSeries
  implicit protected def fromShared(s: SSeries): S

  private def allowedMajors(ds: Array[Dataset], sc: SampleClass): Set[String] = {
    val dsTitles = ds.map(_.getTitle).distinct.toList
    context.samples.datasetURIs = dsTitles.map(Datasets.packURI(_))
    context.samples.attributeValues(scAsScala(sc).filterAll,
      schema.majorParameter()).toSet
  }
  
  def rankedCompounds(ds: Array[Dataset], sc: SampleClass, 
      rules: Array[RankRule]): Array[MatchResult] = {
    val nnr = rules.takeWhile(_ != null)
    var srs = nnr.map(asScala(_))
    var probesRules = nnr.map(_.probe).zip(srs)

    //Convert the input probes (which may actually be gene symbols) into definite probes
    probesRules = probesRules.flatMap(pr => {
      val resolved = context.probes.identifiersToProbes(mcontext.probeMap,  
          Array(pr._1), true, true)
      if (resolved.size == 0) {
        throw new NoSuchProbeException(pr._1)
      }
      resolved.map(r => (r.identifier, pr._2))
    })

    val db = getDB()
    try {      
      val key: S = new SSeries("", probesRules.head._1, "dose_level", sc, Array.empty)

      val ranked = ranking(db, key).rankCompoundsCombined(probesRules)

      val rr = ranked.toList.sortWith((x1, x2) => {
        val (v1, v2) = (x1._3, x2._3)
        if (JDouble.isNaN(v1)) {
          false
        } else if (JDouble.isNaN(v2)) {
          true
        } else {
          v1 > v2
        }
      })
    
      val allowedMajorVals = allowedMajors(ds, sc)
      val mediumVals = schema.sortedValues(schema.mediumParameter())
      
      val r = rr.map(p => {
        val (compound, score, dose) = (p._1, p._3, mediumVals.indexOf(p._2) - 1)        
        new MatchResult(compound, score, dose) 
      }).filter(x => allowedMajorVals.contains(x.compound))

      for (s <- r.take(10)) {
        println(s)
      }
      r.toArray
    } finally {
      db.release()
    }    
  }

  def getSingleSeries(sc: SampleClass, probe: String, timeDose: String, 
      compound: String): SSeries = {
    val db = getDB()
    try {
      val key: S = new SSeries("", probe, "dose_level", sc, Array.empty)           
      asShared(db.read(key).head)
    } finally {
      db.release()
    }
  }

  def getSeries(sc: SampleClass, probes: Array[String], timeDose: String, 
      compounds: Array[String]): JList[SSeries] = {
    val validated = context.probes.identifiersToProbes(mcontext.probeMap, 
        probes, true, true).map(_.identifier)
    val db = getDB()
    try {
      val ss = validated.flatMap(p =>
        compounds.flatMap(c =>
          db.read(fromShared(new SSeries("", p, "dose_level", 
              sc.copyWith("compound_name", c), Array.empty)))))              
      println(s"Read ${ss.size} series")
      println(ss.take(5).mkString("\n"))
      val jss = ss.map(asShared)
      new ArrayList[SSeries](asJavaCollection(jss))
    } finally {
      db.release()
    }
  }

}