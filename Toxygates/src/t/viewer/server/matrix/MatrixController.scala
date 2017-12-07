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

package t.viewer.server.matrix

import java.util.logging.Logger
import t.Context
import t.common.shared.AType
import t.common.shared.DataSchema
import t.common.shared.PerfTimer
import t.common.shared.ValueType
import t.common.shared.sample.Group
import t.db.ExprValue
import t.db.ExtMatrixDB
import t.db.PExprValue
import t.db.TransformingWrapper
import t.db.kyotocabinet.KCMatrixDB
import t.platform.OrthologMapping
import t.viewer.shared.DBUnavailableException
import t.viewer.shared.ManagedMatrixInfo
import t.viewer.server.matrix._
import t.viewer.server.RowLabels
import t.viewer.server.MergedRowLabels
import t.viewer.server.Platforms

object MatrixController {
  def groupPlatforms(context: Context, groups: Seq[Group]): Iterable[String] = {
    val samples = groups.toList.flatMap(_.getSamples().map(_.id))
    context.samples.platforms(samples)
  }

  def apply(context: Context, orthologs: () => Iterable[OrthologMapping],
    groups: Seq[Group], initProbes: Seq[String], typ: ValueType,
    fullLoad: Boolean): MatrixController = {
    val pfs = groupPlatforms(context, groups)
    if (pfs.size > 1) {
      new MergedMatrixController(context, orthologs, groups, initProbes,
          pfs, typ, fullLoad)
    } else {
      new MatrixController(context, groups, initProbes,
        pfs, typ, fullLoad)
    }
  }
}

/**
 * A managed matrix session and associated state.
 * The matrix is loaded automatically when a MatrixController
 * instance is created.
 */
class MatrixController(context: Context,
    val groups: Seq[Group],
    val initProbes: Seq[String],
    val groupPlatforms: Iterable[String],
    val typ: ValueType,
    fullLoad: Boolean) {

  private def probes = context.probes
  protected val platforms = Platforms(probes)

  private implicit val mcontext = context.matrix
  private var sortAssoc: AType = _

  lazy val filteredProbes =
    platforms.filterProbes(initProbes, groupPlatforms)

  protected def platformsForProbes(ps: Iterable[String]): Iterable[String] =
    ps.flatMap(platforms.platformForProbe(_)).toList.distinct

  protected def enhancedCols = true

  protected def makeMatrix(probes: Seq[String],
      typ: ValueType, fullLoad: Boolean): ManagedMatrix = {

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
      //TODO get rid of/factor out this, possibly to a factory of some kind
      //TODO get rid of the enhancedCols flag
      val b = reader match {
        //TODO modify TransformingWrapper API to be able to check for type argument at runtime
        //or stop trying to match the type argument
        case wrapped: TransformingWrapper[PExprValue @unchecked] =>
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

      //TODO best location of this heuristic?
      val sparseRead = probes.size <= 10
      b.build(groups, sparseRead, fullLoad)
    } finally {
      reader.release
    }
  }

  protected def mapper: Option[MatrixMapper] = None

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
      makeMatrix(filteredProbes.toSeq, typ, fullLoad)
    } else {
      val emptyMatrix = new ExprMatrix(List(), 0, 0, Map(), Map(), List())
      new ManagedMatrix(List(), new ManagedMatrixInfo(), emptyMatrix, emptyMatrix, Map())
    }

    pt.mark("MakeMatrix")
    mm.info.setPlatforms(groupPlatforms.toArray)

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
    managedMatrix.selectProbes(useProbes.toSeq)
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

  def rowLabels(schema: DataSchema): RowLabels = new RowLabels(context, schema)
}

/**
 * A matrix controller that applies the MedianValueMapper.
 */
class MergedMatrixController(context: Context,
    orthologs: () => Iterable[OrthologMapping], groups: Seq[Group], initProbes: Seq[String],
    groupPlatforms: Iterable[String],
    typ: ValueType, fullLoad: Boolean)
    extends MatrixController(context, groups, initProbes, groupPlatforms, typ, fullLoad) {

  override protected def enhancedCols = false

  lazy val orth = orthologs().filter(! _.mappings.isEmpty).head

  println(s"Using orthologs from ${orth.name} for mapping")

  override lazy val filteredProbes = {
    //Use orthologs to expand the probe set if this request is
    //multi-platform
    val expanded = initProbes.map(orth.forProbe.getOrElse(_, Set())).flatten
    platforms.filterProbes(expanded, groupPlatforms)
  }

  override protected def mapper: Option[MatrixMapper] = {
    val pm = new OrthologProbeMapper(orth)
    val vm = MedianValueMapper
    Some(new MatrixMapper(pm, vm))
  }

  override def rowLabels(schema: DataSchema) = new MergedRowLabels(context, schema)
}
