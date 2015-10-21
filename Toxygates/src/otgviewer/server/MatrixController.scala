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
import scala.collection.JavaConversions._
import otgviewer.shared.DBUnavailableException
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.Synthetic
import t.Context
import t.common.shared.PerfTimer
import t.common.shared.ValueType
import t.common.shared.probe.MedianValueMapper
import t.common.shared.probe.OrthologProbeMapper
import t.db.MatrixContext
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.kyotocabinet.KCMatrixDB
import t.platform.OrthologMapping
import t.viewer.server.Platforms
import t.common.shared.AType
import t.viewer.shared.table.SortKey
import t.common.shared.sample.ExprMatrix
import t.viewer.server.EVArray
import t.common.shared.sample.ExpressionValue

/**
 * A managed matrix session and associated state.
 * The matrix is loaded automatically when a MatrixController
 * instance is created.
 */
class MatrixController(context: Context,
    orthologs: Iterable[OrthologMapping],
    val groups: Seq[Group], val initProbes: Seq[String],
    typ: ValueType, useStandardMapper: Boolean, sparseRead: Boolean,
    fullLoad: Boolean) {

  private def probes = context.probes
  private def platforms = Platforms(probes)
  private implicit val mcontext = context.matrix
  private var sortAssoc: AType = _

  lazy val groupPlatforms: Iterable[String] = {
    val samples = groups.toList.flatMap(_.getSamples().map(_.id))
    context.samples.platforms(samples)
  }

  lazy val filteredProbes =
    platforms.filterProbes(initProbes, groupPlatforms)

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
      case e: Exception => throw new DBUnavailableException()
    }

    val multiPlat = groupPlatforms.size > 1

    try {
      val enhancedCols = !multiPlat

      val b = reader match {
        case ext: KCExtMatrixDB =>
          assert(typ == ValueType.Folds)
          new ExtFoldBuilder(enhancedCols, ext, probes)
        case db: KCMatrixDB =>
          if (typ == ValueType.Absolute) {
            new NormalizedBuilder(enhancedCols, db, probes)
          } else {
            new FoldBuilder(db, probes)
          }
        case _ => throw new Exception("Unexpected DB reader type")
      }
      b.build(groups, sparseRead, fullLoad)
    } finally {
      reader.release
    }
  }

  protected lazy val standardMapper = {
    if (orthologs.isEmpty) {
      None
    } else {
      val pm = new OrthologProbeMapper(orthologs.head)
      val vm = MedianValueMapper
      Some(new MatrixMapper(pm, vm))
    }
  }

  protected def groupMapper(groups: Iterable[Group]): Option[MatrixMapper] = {
    val os = groups.flatMap(_.collect("organism")).toSet
    println("Detected species in groups: " + os)
    if (os.size > 1) {
      standardMapper
    } else {
      None
    }
  }

  protected def applyMapper(mm: ManagedMatrix, mapper: Option[MatrixMapper]): ManagedMatrix = {
    mapper match {
      case Some(m) => m.convert(mm)
      case None    => mm
    }
  }

  var managedMatrix: ManagedMatrix = {
    val pt = new PerfTimer(Logger.getLogger("matrixController.loadMatrix"))
    
    val mm = if (filteredProbes.size > 0) {
      makeMatrix(filteredProbes, typ, sparseRead, fullLoad)
    } else {
      val emptyMatrix = new ExprMatrix(List(), 0, 0,Map(), Map(), List())
      new ManagedMatrix(List(), new ManagedMatrixInfo(), emptyMatrix, emptyMatrix)
    }
    
    pt.mark("MakeMatrix")
    mm.info.setPlatforms(groupPlatforms.toArray)

    val mapper = if (useStandardMapper) {
      if (groupPlatforms.size > 1) {
        standardMapper
      } else {
        None
      }
    } else {
      groupMapper(groups)
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
    if (!probes.isEmpty) {
      println("Refilter probes: " + probes.length)
      managedMatrix.selectProbes(probes)
    } else {
      val info = managedMatrix.info
      val groups = (0 until info.numDataColumns()).map(i => info.columnGroup(i))
      val allProbes = platforms.filterProbes(List(), groupPlatforms).toArray
      managedMatrix.selectProbes(allProbes)
    }
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
          case Some(v) => new ExpressionValue(v)
          case None    => new ExpressionValue(Double.NaN, 'A')
        })
      val evas = evs.map(v => EVArray(Seq(v)))
      ExprMatrix.withRows(evas, rowKeys, List("POPSEQ"))
    } else {
      throw new Exception(s"No sort key for $ass")
    }
  }

}
