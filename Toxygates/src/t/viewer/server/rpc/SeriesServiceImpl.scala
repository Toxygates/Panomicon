/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.viewer.server.rpc

import java.util.ArrayList
import java.util.{List => JList}

import scala.Array.canBuildFrom
import scala.collection.JavaConversions._
import scala.language.implicitConversions

import otgviewer.server.rpc.Conversions.asScala
import otgviewer.shared.MatchResult
import otgviewer.shared.RankRule
import otgviewer.shared.{Series => SSeries}
import t.SeriesRanking
import t.common.shared.Dataset
import t.db.MatrixContext
import t.db.Series
import t.db.SeriesDB
import t.model.SampleClass
import t.sparql.Datasets
import t.sparql.SampleClassFilter
import t.sparql.SampleFilter
import t.viewer.client.rpc.SeriesService
import t.viewer.server.Configuration
import t.viewer.server.Conversions._
import t.viewer.shared.NoSuchProbeException

abstract class SeriesServiceImpl[S <: Series[S]] extends TServiceServlet with SeriesService {
  import java.lang.{ Double => JDouble }

  private var config: Configuration = _
  private implicit def mcontext: MatrixContext = context.matrix
  implicit protected def context: t.Context

  protected def getDB(): SeriesDB[S]

  protected def ranking(db: SeriesDB[S], key: S): SeriesRanking[S] = new SeriesRanking(db, key)

  implicit protected def asShared(s: S): SSeries
  implicit protected def fromShared(s: SSeries): S

  protected def attributes = baseConfig.attributes

  override def localInit(config: Configuration): Unit = {
    this.config = config
  }

  private def allowedMajors(ds: Array[Dataset], sc: SampleClass): Set[String] = {
    val dsTitles = ds.map(_.getTitle).distinct.toList
    implicit val sf = SampleFilter(instanceURI = config.instanceURI,
        datasetURIs = dsTitles.map(Datasets.packURI(_)))

    val majAttr = attributes.byId(schema.majorParameter())
    context.samples.attributeValues(SampleClassFilter(sc).filterAll,
      majAttr).toSet
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
