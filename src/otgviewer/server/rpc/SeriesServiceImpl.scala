package otgviewer.server.rpc

import java.lang.{Double => JDouble}
import java.util.ArrayList
import java.util.{List => JList}
import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asJavaCollection
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import Conversions.asJava
import Conversions.asScala
import Conversions.speciesFromFilter
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import otg.OTGContext
import otg.SeriesRanking
import otg.sparql.AffyProbes
import otgviewer.client.rpc.SeriesService
import otgviewer.server.Configuration
import otgviewer.shared.DataFilter
import otgviewer.shared.MatchResult
import otgviewer.shared.NoSuchProbeException
import otgviewer.shared.RankRule
import otgviewer.shared.Series
import t.db.SeriesDB
import t.db.kyotocabinet.KCSeriesDB
import otg.OTGContext
import otg.SeriesRanking
import t.db.SeriesDB
import otg.OTGSeries
import scala.annotation.tailrec
import otg.TimeDose
import otgviewer.shared.TimesDoses
import t.BaseConfig

class SeriesServiceImpl extends RemoteServiceServlet with SeriesService {
  import Conversions._
  import scala.collection.JavaConversions._  

  import java.lang.{ Double => JDouble }

  private implicit var context: OTGContext = _
  var affyProbes: AffyProbes = _
  var baseConfig: BaseConfig = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }

  // Useful for testing
  def localInit(config: Configuration) {
    val homePath = config.toxygatesHomeDir
    baseConfig = config.baseConfig
    context = config.context
    
    affyProbes = new AffyProbes(context.triplestoreConfig.triplestore)
  }

  override def destroy() {
    affyProbes.close()      
    super.destroy()
  }
  
  private def getDB(): SeriesDB[OTGSeries] = {
    new KCSeriesDB(baseConfig.data.seriesDb, OTGSeries, false) {
    	override def read(key: OTGSeries): Iterable[OTGSeries] = {
    	  OTGSeries.normalize(super.read(key))
    	}
    }
  }
  
  def rankedCompounds(filter: DataFilter, rules: Array[RankRule]): Array[MatchResult] = {
    val nnr = rules.takeWhile(_ != null)
    var srs = nnr.map(asScala(_))
    var probesRules = nnr.map(_.probe).zip(srs)

    //Convert the input probes (which may actually be genes) into definite probes
    probesRules = probesRules.flatMap(pr => {
      val resolved = affyProbes.identifiersToProbes(context.unifiedProbes,  
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
      val key = asScala(filter,
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
        TimesDoses.allDoses.indexOf(p._2) - 1))

      for (s <- r.take(10)) {
        println(s)
      }
      r.toArray
    } finally {
      db.close()
    }    
  }

  def getSingleSeries(filter: DataFilter, probe: String, timeDose: String, compound: String): Series = {
    val db = getDB()
    try {
      db.read(asScala(filter, new Series("", probe, timeDose, compound, Array.empty))).head
    } finally {
      db.close()
    }
  }

  def getSeries(filter: DataFilter, probes: Array[String], timeDose: String, compounds: Array[String]): JList[Series] = {
    val validated = affyProbes.identifiersToProbes(context.unifiedProbes, 
        probes, true, true).map(_.identifier)
    val db = getDB()
    try {
      val ss = validated.flatMap(p =>
        compounds.flatMap(c =>
          db.read(asScala(filter, new Series("", p, timeDose, c, Array.empty)))))
      println(s"Read ${ss.size} series")
      println(ss.take(5).mkString("\n"))
      val jss = ss.map(asJava(_))
      new ArrayList[Series](asJavaCollection(jss))
    } finally {
      db.close()
    }
  }
}