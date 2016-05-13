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

package otgviewer.server

import java.util.logging.Logger
import scala.collection.JavaConversions.asScalaSet
import otgviewer.shared.DBUnavailableException
import t.viewer.shared.ManagedMatrixInfo
import t.Context
import t.common.shared.AType
import t.common.shared.PerfTimer
import t.common.shared.ValueType
import t.common.shared.probe.MedianValueMapper
import t.common.shared.probe.OrthologProbeMapper
import t.viewer.server.ExprMatrix
import t.common.shared.sample.ExpressionValue
import t.common.shared.sample.Group
import t.db.PExprValue
import t.db.TransformingWrapper
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.kyotocabinet.KCMatrixDB
import t.platform.OrthologMapping
import t.viewer.server.Platforms
import t.viewer.shared.table.SortKey
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.common.shared.sample.EVArray
import t.db.ExprValue
import t.db.ExtMatrixDB
import t.viewer.server.ManagedMatrix
import t.viewer.server.NormalizedBuilder
import t.viewer.server.ExtFoldBuilder

/**
 * A managed matrix session and associated state.
 * The matrix is loaded automatically when a MatrixController
 * instance is created.
 *
 * TODO move to t.viewer.server
 */
class MatrixController(context: Context,
    orthologs: () => Iterable[OrthologMapping],
    val groups: Seq[Group], val initProbes: Seq[String],
    typ: ValueType, sparseRead: Boolean,
    fullLoad: Boolean) {

  private def probes = context.probes
  private def platforms = Platforms(probes)

  private implicit val mcontext = context.matrix
  private var sortAssoc: AType = _

  lazy val groupPlatforms: Iterable[String] = {
    val samples = groups.toList.flatMap(_.getSamples().map(_.id))
    context.samples.platforms(samples)
  }

  def multiPlatform = groupPlatforms.size > 1

  lazy val filteredProbes = {
    val expanded = if (multiPlatform) {
      //Use orthologs to expand the probe set if this request is
      //multi-platform
      val or = orthologs().head
      initProbes.map(or.forProbe.getOrElse(_, Set())).flatten
    } else {
      initProbes
    }
    platforms.filterProbes(expanded, groupPlatforms)
  }

  protected def platformsForProbes(ps: Iterable[String]): Iterable[String] =
    ps.flatMap(platforms.platformForProbe(_)).toList.distinct

  protected def makeMatrix(probes: Seq[String],
      typ: ValueType, sparseRead: Boolean, fullLoad: Boolean): ManagedMatrix = {

    val reader = try {
      if (typ == ValueType.Absolute) {
        mcontext.absoluteDBReader
      } else {
        mcontext.foldsDBReader
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new DBUnavailableException(e)
    }

    try {
      val enhancedCols = !multiPlatform

      //TODO get rid of/factor out this, possibly to a factory of some kind
      val b = reader match {
        case wrapped: TransformingWrapper[PExprValue] =>
          new ExtFoldBuilder(enhancedCols, wrapped, probes)
        case db: KCMatrixDB =>
          assert(typ == ValueType.Absolute)
          new NormalizedBuilder(enhancedCols, db, probes)
        case db: ExtMatrixDB =>
          if (typ == ValueType.Absolute) {
            new NormalizedBuilder(enhancedCols, db, probes)
          } else {
            new ExtFoldBuilder(enhancedCols, db, probes)
          }
        case _ => throw new Exception("Unexpected DB reader type")
      }
      b.build(groups, sparseRead, fullLoad)
    } finally {
      reader.release
    }
  }

  protected lazy val standardMapper = {
    if (orthologs().isEmpty) {
      None
    } else {
      val pm = new OrthologProbeMapper(orthologs().head)
      val vm = MedianValueMapper
      Some(new MatrixMapper(pm, vm))
    }
  }

  protected def applyMapper(mm: ManagedMatrix, mapper: Option[MatrixMapper]): ManagedMatrix = {
    mapper match {
      case Some(m) =>
        println(s"Apply mapper: $m")
        m.convert(mm)
      case None    =>
        println("No mapper being applied")
        mm
    }
  }

  val managedMatrix: ManagedMatrix = {
    val pt = new PerfTimer(Logger.getLogger("matrixController.loadMatrix"))

    val mm = if (filteredProbes.size > 0) {
      makeMatrix(filteredProbes, typ, sparseRead, fullLoad)
    } else {
      val emptyMatrix = new ExprMatrix(List(), 0, 0, Map(), Map(), List())
      new ManagedMatrix(List(), new ManagedMatrixInfo(), emptyMatrix, emptyMatrix, Map())
    }

    pt.mark("MakeMatrix")
    mm.info.setPlatforms(groupPlatforms.toArray)

    val mapper = if (multiPlatform) {
      standardMapper
    } else {
      None
    }

    val mm2 = applyMapper(mm, mapper)
    pt.mark("ApplyMapper")
    pt.finish
    mm2
  }

  /**
   * Select probes and update the current managed matrix
   */
  def selectProbes(probes: Seq[String]): ManagedMatrix = {
    val useProbes = (if (!probes.isEmpty) {
      println("Refilter probes: " + probes.length)
      probes
    } else {
      //all probes
      platforms.filterProbes(List(), groupPlatforms)
    })
    managedMatrix.selectProbes(useProbes)
    managedMatrix
  }

  /**
   * Sort rows
   */
  def applySorting(sortKey: SortKey, ascending: Boolean): ManagedMatrix = {
    sortKey match {
      case mc: SortKey.MatrixColumn =>
        if (Some(mc.matrixIndex) != managedMatrix.sortColumn ||
          ascending != managedMatrix.sortAscending) {
          managedMatrix.sort(mc.matrixIndex, ascending)
          println("SortCol: " + mc.matrixIndex + " asc: " + ascending)
        }
      case as: SortKey.Association =>
        val old = sortAssoc
        val nw = as.atype
        if (old == null || nw != old
          || ascending != managedMatrix.sortAscending) {
          sortAssoc = nw
          val st = assocSortTable(as.atype,
            managedMatrix.probesForAuxTable)
          managedMatrix.sortWithAuxTable(st, ascending)
          println("Sort with aux table for " + as)
        }
    }
    managedMatrix
  }

  protected def assocSortTable(ass: AType, rowKeys: Seq[String]): ExprMatrix = {
    val key = ass.auxSortTableKey
    if (key != null) {
      val sm = context.auxSortMap(key)
      println("Rows: " + rowKeys.take(10))
      println("aux: " + sm.take(10))
      val evs = rowKeys.map(r =>
        sm.get(r) match {
          case Some(v) => ExprValue(v)
          case None    => ExprValue(Double.NaN, 'A')
        })
      ExprMatrix.withRows(evs.map(ev => Seq(ev)), rowKeys, List("POPSEQ"))
    } else {
      throw new Exception(s"No sort key for $ass")
    }
  }

}
