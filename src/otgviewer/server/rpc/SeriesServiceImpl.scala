package otgviewer.server.rpc

import java.lang.{Double => JDouble}
import java.util.ArrayList
import java.util.{List => JList}
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asJavaCollection
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Conversions.asJava
import Conversions.asScala
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.OTGContext
import otg.SeriesRanking
import otg.sparql.Probes
import otgviewer.client.rpc.SeriesService
import t.viewer.server.Configuration
import otgviewer.shared.DataFilter
import otgviewer.shared.MatchResult
import otgviewer.shared.NoSuchProbeException
import otgviewer.shared.RankRule
import otgviewer.shared.Series
import t.db.SeriesDB
import t.db.kyotocabinet.KCSeriesDB
import otg.OTGSeries
import t.BaseConfig
import t.common.shared.SampleClass
import t.DataConfig
import otg.OTGBConfig
import t.TriplestoreConfig
import otgviewer.shared.DBUnavailableException
import t.global.KCDBRegistry
import otgviewer.shared.OTGSchema
import otg.OTGContext
import t.db.MatrixContext

class SeriesServiceImpl extends RemoteServiceServlet with SeriesService {
  import Conversions._
  import scala.collection.JavaConversions._  

  import java.lang.{ Double => JDouble }

  private implicit var context: otg.Context = _ 
  private implicit var mcontext: OTGContext = _
  
  var affyProbes: Probes = _
  var baseConfig: BaseConfig = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }

  // Useful for testing
  def localInit(config: Configuration) {
    val homePath = config.toxygatesHomeDir
    baseConfig = baseConfig(config.tsConfig, config.dataConfig)
    
    //TODO avoid initializing Context in each service 
    context = otg.Context(baseConfig)
    mcontext = context.matrix
    affyProbes = context.probes
  }
  
  def baseConfig(ts: TriplestoreConfig, data: DataConfig): BaseConfig = 
    OTGBConfig(ts, data)

  override def destroy() {
    affyProbes.close()      
    super.destroy()
  }
  
  private def getDB(): SeriesDB[OTGSeries] = {
    //TODO organise this better
    try {
    val file = baseConfig.data.seriesDb
    val db = KCDBRegistry.get(file, false)    
    db match {
      case Some(d) => new KCSeriesDB(file, d, OTGSeries) {
        override def read(key: OTGSeries): Iterable[OTGSeries] = {
    	  OTGSeries.normalize(super.read(key))
    	}
      }
      case None => throw new Exception("Unable to get DB")
    }  
    } catch {
      case e: Exception => throw new DBUnavailableException()
    }
  }
  
  def rankedCompounds(sc: SampleClass, rules: Array[RankRule]): Array[MatchResult] = {
    val nnr = rules.takeWhile(_ != null)
    var srs = nnr.map(asScala(_))
    var probesRules = nnr.map(_.probe).zip(srs)

    //Convert the input probes (which may actually be genes) into definite probes
    probesRules = probesRules.flatMap(pr => {
      val resolved = affyProbes.identifiersToProbes(mcontext.unifiedProbes,  
          Array(pr._1), true, true)
      if (resolved.size == 0) {
        throw new NoSuchProbeException(pr._1)
      }
      resolved.map(r => (r.identifier, pr._2))
    })

    val db = getDB()
    try {
      //TODO: probe is actually irrelevant here but the API is not well designed
      //Same for timeDose = High
      val key = asScala(sc,
        new otgviewer.shared.Series("", probesRules.head._1, "High", null, Array.empty))
      val ranking = new SeriesRanking(db, key)
      val ranked = ranking.rankCompoundsCombined(probesRules)

      val rr = ranked.toList.sortWith((x1, x2) => {
        if (JDouble.isNaN(x1._3)) {
          false
        } else if (JDouble.isNaN(x2._3)) {
          true
        } else {
          x1._3 > x2._3
        }
      })
      
      val r = rr.map(p => new MatchResult(p._1, p._3, 
        OTGSchema.allDoses.indexOf(p._2) - 1))

      for (s <- r.take(10)) {
        println(s)
      }
      r.toArray
    } finally {
      db.release()
    }    
  }

  def getSingleSeries(sc: SampleClass, probe: String, timeDose: String, compound: String): Series = {
    val db = getDB()
    try {
      db.read(asScala(sc, new Series("", probe, timeDose, compound, Array.empty))).head
    } finally {
      db.release()
    }
  }

  def getSeries(sc: SampleClass, probes: Array[String], timeDose: String, compounds: Array[String]): JList[Series] = {
    val validated = affyProbes.identifiersToProbes(mcontext.unifiedProbes, 
        probes, true, true).map(_.identifier)
    val db = getDB()
    try {
      val ss = validated.flatMap(p =>
        compounds.flatMap(c =>
          db.read(asScala(sc, new Series("", p, timeDose, c, Array.empty)))))
      println(s"Read ${ss.size} series")
      println(ss.take(5).mkString("\n"))
      val jss = ss.map(asJava(_))
      new ArrayList[Series](asJavaCollection(jss))
    } finally {
      db.release()
    }
  }
}