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

class SeriesServiceImpl extends RemoteServiceServlet with SeriesService {
  import Conversions._
  import scala.collection.JavaConversions._  

  import java.lang.{ Double => JDouble }

  private var db: SeriesDB = _
  private implicit var context: OTGContext = _
  var affyProbes: AffyProbes = _
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)
    localInit(Configuration.fromServletConfig(config))
  }

  // Useful for testing
  def localInit(config: Configuration) {
    val homePath = config.toxygatesHomeDir
    context = config.context
    db = new KCSeriesDB(homePath + "/otgfs.kct")
    println("Series DB is open")
    affyProbes = new AffyProbes(context.triplestoreConfig.triplestore)
  }

  override def destroy() {
    affyProbes.close()
    db.close()    
    super.destroy()
  }

  def rankedCompounds(filter: DataFilter, rules: Array[RankRule]): Array[MatchResult] = {
    val nnr = rules.takeWhile(_ != null)
    var srs = nnr.map(asScala(_))
    var probesRules = nnr.map(_.probe).zip(srs)

    //Convert the input probes (which may actually be genes) into definite probes
    probesRules = probesRules.flatMap(pr => {
      val resolved = affyProbes.identifiersToProbes(filter, Array(pr._1), true, true)
      if (resolved.size == 0) {
        throw new NoSuchProbeException(pr._1)
      }
      resolved.map(r => (r.identifier, pr._2))
    })

    //TODO: probe is actually irrelevant here but the API is not well designed
    //Same for timeDose = High
    val key = asScala(filter, new otgviewer.shared.Series("", probesRules.head._1, "High", null, Array.empty))
    val ranking = new SeriesRanking(db, filter, key)
    val ranked = ranking.rankCompoundsCombined(probesRules)
    val r = ranked.map(p => new MatchResult(p._1, p._2._1, p._2._2)).toArray
    val rr = r.sortWith((x1, x2) => {
      if (JDouble.isNaN(x1.score)) {
        false
      } else if (JDouble.isNaN(x2.score)) {
        true
      } else {
        x1.score > x2.score
      }
    })

    for (s <- rr.take(10)) {
      println(s)
    }
    rr
  }

  def getSingleSeries(filter: DataFilter, probe: String, timeDose: String, compound: String): Series = {
    db.read(asScala(filter, new Series("", probe, timeDose, compound, Array.empty))).head
  }

  def getSeries(filter: DataFilter, probes: Array[String], timeDose: String, compounds: Array[String]): JList[Series] = {
    val validated = affyProbes.identifiersToProbes(filter, probes, true, true).map(_.identifier)
    val ss = validated.flatMap(p =>
      compounds.flatMap(c =>
        db.read(asScala(filter, new Series("", p, timeDose, c, Array.empty)))))
    val jss = ss.map(asJava(_))
    new ArrayList[Series](asJavaCollection(jss))
  }
}