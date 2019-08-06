/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import otg.viewer.server.rpc.Conversions._
import t.Context
import t.common.shared._
import t.common.shared.sample.Group
import t.db.ExtMatrixDB
import t.db.PExprValue
import t.db.TransformingWrapper
import t.platform.OrthologMapping

import t.viewer.server.Conversions._
import t.viewer.shared.DBUnavailableException
import t.viewer.shared.ManagedMatrixInfo
import t.viewer.server.matrix._
import t.viewer.server.Platforms
import t.viewer.shared.SortKey
import t.common.shared.sample.ExpressionRow
import t.db.MatrixContext

object MatrixController {
  def groupPlatforms(context: Context, groups: Seq[Group]): Iterable[String] = {
    val samples = groups.toList.flatMap(_.getSamples().map(_.id))
    context.samples.platforms(samples)
  }

  def apply(context: Context, orthologs: () => Iterable[OrthologMapping],
    groups: Seq[Group], initProbes: Seq[String], typ: ValueType,
    fullLoad: Boolean): MatrixController = {
    val pfs = groupPlatforms(context, groups)
    val params = ControllerParams(context, groups, initProbes,
          pfs, typ, fullLoad)
    if (pfs.size > 1) {
      new MergedMatrixController(params, orthologs)
    } else {
      new DefaultMatrixController(params)
    }
  }
}

object ControllerParams {
  def apply(context: Context, groups: Seq[Group],
    initProbes: Seq[String], groupPlatforms: Iterable[String],
    typ: ValueType, fullLoad: Boolean): ControllerParams =
      ControllerParams(context.matrix,
      Platforms(context.probes), groups, initProbes, groupPlatforms, typ,
      fullLoad)
}

/**
 * Principal parameters that a matrix controller needs to load a matrix.
 */
case class ControllerParams(val matrixContext: MatrixContext,
    val platforms: Platforms,
    val groups: Seq[Group],
    val initProbes: Seq[String],
    val groupPlatforms: Iterable[String],
    val typ: ValueType,
    fullLoad: Boolean)

/**
 * A managed matrix session and associated state.
 * The matrix is loaded automatically when a MatrixController
 * instance is created.
 */
abstract class MatrixController(params: ControllerParams) {
  val groups = params.groups
  val initProbes = params.initProbes
  val groupPlatforms = params.groupPlatforms
  val typ = params.typ

  /**
   * The type of the matrix that is managed.
   */
  type Mat <: ManagedMatrix

  protected def platforms = params.platforms

  private implicit val mcontext = params.matrixContext

  def groupSpecies = groups.headOption.flatMap(g => asSpecies(g.getSamples()(0).sampleClass()))

  lazy val filteredProbes =
    platforms.filterProbes(initProbes, groupPlatforms, groupSpecies)

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

  lazy val managedMatrix = _managedMatrix

  def _managedMatrix: Mat = {

    val mm = if (filteredProbes.size > 0) {
      finish(makeMatrix(filteredProbes.toSeq, typ, params.fullLoad))
    } else {
      val emptyMatrix = new ExprMatrix(Vector(), 0, 0, Map(), Map(), List())
      finish(new ManagedMatrix(
        LoadParams(List(), new ManagedMatrixInfo(), emptyMatrix, emptyMatrix, Map())
        ))
    }
    mm.info.setPlatforms(groupPlatforms.toArray)
    mm
  }

  /**
   * Construct the expected matrix type from a ManagedMatrix.
   */
  protected def finish(mm: ManagedMatrix): Mat

  /**
   * Select probes and update the current managed matrix
   */
  def selectProbes(probes: Seq[String]): Mat = {
    val useProbes = (if (!probes.isEmpty) {
      println("Refilter probes: " + probes.length)
      probes
    } else {
      println("Select all probes")
      platforms.filterProbes(List(), groupPlatforms, groupSpecies)
    })
    managedMatrix.selectProbes(useProbes.toSeq)
    managedMatrix
  }

  /**
   * Sort rows
   */
  def applySorting(sortKey: SortKey, ascending: Boolean): Mat = {
    sortKey match {
      case mc: SortKey.MatrixColumn =>
        if (Some(mc.matrixIndex) != managedMatrix.sortColumn ||
          ascending != managedMatrix.sortAscending) {
          managedMatrix.sort(mc.matrixIndex, ascending)
          println("SortCol: " + mc.matrixIndex + " asc: " + ascending)
        }
      case _ => throw new Exception("Unsupported sort method")
    }
    managedMatrix
  }

  protected def rowLabels(context: Context, schema: DataSchema): RowLabels = new RowLabels(context, schema)

  def insertAnnotations(context: Context, schema: DataSchema,
      rows: Seq[ExpressionRow],
      withSymbols: Boolean): Seq[ExpressionRow] = {
    val rl = rowLabels(context, schema)
    rl.insertAnnotations(rows, withSymbols)
  }
}

/**
 * A controller that produces ManagedMatrix instances and can apply a mapper.
 */
class DefaultMatrixController(params: ControllerParams) extends MatrixController(params) {
  type Mat = ManagedMatrix
  def finish(mm: ManagedMatrix) = mm

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

  override def _managedMatrix = {
    applyMapper(super._managedMatrix, mapper)
  }
}

/**
 * A matrix controller that applies the MedianValueMapper.
 */
class MergedMatrixController(params: ControllerParams, orthologs: () => Iterable[OrthologMapping])
    extends DefaultMatrixController(params) {

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

  override protected def rowLabels(context: Context, schema: DataSchema) = new MergedRowLabels(context, schema)
}
