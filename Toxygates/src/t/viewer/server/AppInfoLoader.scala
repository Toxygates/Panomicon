package t.viewer.server

import t.sparql.Probes
import t.common.shared.clustering.ProbeClustering
import t.common.shared.StringList
import t.viewer.shared.AppInfo
import scala.collection.JavaConversions._
import t.common.shared.Platform
import t.BaseConfig
import t.sparql._
import t.util.PeriodicRefresh
import t.util.Refreshable
import t.viewer.shared.mirna.MirnaSource
import t.viewer.server.Conversions._

class AppInfoLoader(probeStore: Probes,
                    configuration: Configuration,
                    baseConfig: BaseConfig,
                    appName: String) {

  /**
   * Called when AppInfo needs a full refresh.
   */
  def load(): AppInfo = {
    val probeLists = predefProbeLists()

    new AppInfo(configuration.instanceName, Array(),
      sPlatforms(), probeLists,
      configuration.intermineInstances.toArray,
      probeClusterings(probeLists), appName,
      makeUserKey(), getAnnotationInfo,
      baseConfig.attributes,
      getMirnaSourceInfo)
  }

  def predefProbeLists() = {
    val ls = probeStore.probeLists(configuration.instanceURI).
      mapInnerValues(p => p.identifier)
    val sls = ls.map(x => new StringList(
      StringList.PROBES_LIST_TYPE, x._1, x._2.toArray)).toList
    new java.util.LinkedList(seqAsJavaList(sls.sortBy(_.name)))
  }

  //Currently identical to predef probe lists
  def probeClusterings(probeLists: Iterable[StringList]) = {
    val cls = probeLists.map(x => ProbeClustering.buildFrom(x))
    new java.util.LinkedList(seqAsJavaList(cls.toSeq))
  }

  /**
   * Obtain data sources information for AppInfo
   */
  private def getAnnotationInfo: Array[Array[String]] = {
    val dynamic = probeStore.annotationsAndComments.toArray
    val static = staticAnnotationInfo
    Array(
      (dynamic ++ static).map(_._1),
      (dynamic ++ static).map(_._2))
  }

  def staticAnnotationInfo: Seq[(String, String)] = Seq()

  /**
   * Generate a new user key, to be used when the client does not already have one.
   */
  def makeUserKey(): String = {
    val time = System.currentTimeMillis()
    val random = (Math.random * Int.MaxValue).toInt
    "%x%x".format(time, random)
  }

  /**
   * "shared" platforms
   */
  def sPlatforms(): Array[Platform] = {
    val platforms = new t.sparql.Platforms(baseConfig) with SharedPlatforms
    platforms.sharedList.toArray
  }

    protected def getMirnaSourceInfo: Array[MirnaSource] = {
    val dynamic = probeStore.mirnaSources.map(s =>
      new MirnaSource(s._1, s._2, s._3, s._4, asJDouble(s._5), s._6.getOrElse(0)))
    val static = staticMirnaSources
    dynamic.toArray ++ static
  }

  /**
   * MiRNA sources that are hardcoded into the application.
   */
  protected def staticMirnaSources: Seq[MirnaSource] = Seq()
}
