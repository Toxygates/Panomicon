/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.server.viewer.rpc

import t.db._
import t.model.SampleClass
import t.model.sample.OTGAttribute
import t.server.common.GWTUtils._
import t.server.viewer.Configuration
import t.server.viewer.rpc.Conversions.asScala
import t.shared.common.{Dataset, SeriesType}
import t.shared.viewer.{MatchResult, NoSuchProbeException, RankRule}
import t.sparql._
import t.util.SafeMath
import t.gwt.viewer.client.rpc.SeriesService
import t._

import scala.language.implicitConversions

class SeriesServiceImpl extends TServiceServlet with SeriesService {
  private var config: Configuration = _
  protected implicit def mcontext: MatrixContext = context.matrix
  implicit protected def implicitContext: Context = context

  protected def getDB(seriesType: SeriesType): SeriesDB[OTGSeries] = {
    import SeriesType._
    seriesType match {
      case Time => mcontext.timeSeriesDBReader
      case Dose => mcontext.doseSeriesDBReader
    }
  }

  protected def ranking(db: SeriesDB[OTGSeries], key: OTGSeries): SeriesRanking =
    new t.SeriesRanking(db, key)

  implicit def asShared(s: OTGSeries): t.shared.viewer.Series = asShared(s, "")
  protected def asShared(s: OTGSeries, geneSym: String): t.shared.viewer.Series =
    Conversions.asJava(s, geneSym)
  protected def fromShared(s: t.shared.viewer.Series): OTGSeries =
    Conversions.asScala(s)

  protected def builder(s: SeriesType): OTGSeriesBuilder = {
    import SeriesType._
    s match {
      case Dose => OTGDoseSeriesBuilder
      case Time => OTGTimeSeriesBuilder
    }
  }

  protected def attributes = baseConfig.attributes

  override def localInit(config: Configuration): Unit = {
    super.localInit(config)
    this.config = config
  }

  private def allowedMajors(ds: Array[Dataset], sc: SampleClass): Set[String] = {
    val ids = ds.map(_.getId).distinct.toList
    val sf = SampleFilter(instanceURI = config.instanceURI,
        datasetURIs = ids.map(DatasetStore.packURI(_)))

    val majAttr = schema.majorParameter()

    context.sampleStore.attributeValues(SampleClassFilter(sc).filterAll,
      majAttr, sf).toSet
  }

  final private def withDB[T](seriesType: SeriesType, f: SeriesDB[OTGSeries] => T): T = {
    val db = getDB(seriesType)
    try {
      f(db)
    } finally {
      db.release()
    }
  }

  def rankedCompounds(seriesType: SeriesType,
      datasets: Array[Dataset], sc: SampleClass,
      rules: Array[RankRule]): Array[MatchResult] = {
    val nrules = rules.takeWhile(_ != null)
    var srules = nrules.map(asScala(_))
    var probesRules = nrules.map(_.probe).zip(srules)

    //Convert the input probes (which may actually be gene symbols) into definite probes
    probesRules = probesRules.flatMap(pr => {
      val resolved = context.probeStore.identifiersToProbes(mcontext.probeMap,
          Array(pr._1), true, true)
      if (resolved.size == 0) {
        throw new NoSuchProbeException(pr._1)
      }
      resolved.map(r => (r.identifier, pr._2))
    })

    withDB(seriesType, db => {
      val key: OTGSeries = new t.shared.viewer.Series("", probesRules.head._1, "",
          OTGAttribute.ExposureTime, sc, Array.empty)

      val ranked = ranking(db, key).rankCompoundsCombined(probesRules)

      val byName = ranked.toSeq.sortBy(_._1)

       /*
        * Since the sort is stable, equal scores will retain the by name ordering from above
        */
      val byScore = byName.sortWith((x1, x2) =>
        SafeMath.safeIsGreater(x1._3, x2._3)
      )

      val allowedMajorVals = allowedMajors(datasets, sc)
      val fixedAttrVals = schema.sortedValues(seriesType.fixedAttribute)

      val r = byScore.map(p => new MatchResult(p._1, p._3, p._2)).
        filter(x => allowedMajorVals.contains(x.compound))

      for (s <- r.take(10)) {
        println(s)
      }
      r.toArray
    })
  }

  def getSingleSeries(seriesType: SeriesType,
      sc: SampleClass, probe: String, timeDose: String,
      compound: String): t.shared.viewer.Series = {
    withDB(seriesType, db => {
      val key: OTGSeries = new t.shared.viewer.Series("", probe, "", seriesType.independentAttribute, sc, Array.empty)
      db.read(key).head
    })
  }

  def getSeries(
    seriesType: SeriesType,
    sc: SampleClass, probes: Array[String], timeDose: String,
    compounds: Array[String]): GWTList[t.shared.viewer.Series] = {
    val validated = context.probeStore.identifiersToProbes(
      mcontext.probeMap, probes, true, true)
    val lookup = Map() ++ context.probeStore.withAttributes(validated).
      map(p => p.identifier -> p.symbols.head)

    val preFilter = withDB(seriesType, db => {
      validated.flatMap(p =>
        compounds.flatMap(c =>
          db.read(fromShared(new t.shared.viewer.Series("", p.identifier, "",
              seriesType.independentAttribute,
            sc.copyWith(OTGAttribute.Compound, c), Array.empty)))))
    })

    /*
     * We should remove series where the fixed attribute is Control from the database,
     * and then remove this check.
     */
    val filtered = preFilter.filter(s =>
      !schema.isControlValue(s.constraints(seriesType.fixedAttribute())))

    println(s"Read ${preFilter.size} series, filtered to ${filtered.size}")
    val javaSeries = filtered.map(s => asShared(s, lookup(s.probeStr)))
    javaSeries.asGWT
  }

  def expectedIndependentPoints(stype: SeriesType, s: t.shared.viewer.Series): Array[String] =
    builder(stype).expectedIndependentVariablePoints(fromShared(s)).toArray
}
