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

package t.viewer.server.matrix

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
import t.viewer.server.PlatformRegistry
import t.viewer.shared.SortKey
import t.model.sample.CoreParameter

object MatrixController {
  private def noOrthologs = () => Iterable.empty

  def apply(context: Context, groups: Seq[Group], initProbes: Seq[String], typ: ValueType,
            orthologs: () => Iterable[OrthologMapping] = noOrthologs): MatrixController = {

    val params = ControllerParams(groups, initProbes, typ)
    val platforms = PlatformRegistry(context.probeStore)

    if (params.platforms(context).size > 1) {
      new MergedMatrixController(context, platforms, params, orthologs)
    } else {
      new DefaultMatrixController(context, platforms, params)
    }
  }
}

/**
 * Principal parameters that a matrix controller needs to load a matrix.
 */
case class ControllerParams(val groups: Seq[Group],
                            val initProbes: Seq[String],
                            val typ: ValueType) {

  def platforms(context: Context): Iterable[String] = {
    val samples = groups.toList.flatMap(_.getSamples)
    if (samples.exists(!_.sampleClass.contains(CoreParameter.Platform))) {
      val ids = samples.map(_.id)
      context.sampleStore.platforms(ids)
    } else {
      samples.map(_.sampleClass.get(CoreParameter.Platform)).distinct
    }
  }
}
/**
 * A managed matrix session and associated state.
 * The matrix is loaded automatically when a MatrixController
 * instance is created.
 */
abstract class MatrixController(context: Context,
                                platforms: PlatformRegistry,
                                params: ControllerParams) {

  def matrixContext = context.matrix
  val groups = params.groups
  val initProbes = params.initProbes
  val groupPlatforms = params.platforms(context)
  val typ = params.typ

  /**
   * The type of the matrix that is managed.
   */
  type Mat <: ManagedMatrix

  def groupSpecies = groups.headOption.flatMap(g => asSpecies(g.getSamples()(0).sampleClass()))

  lazy val filteredProbes =
    platforms.filterProbes(initProbes, groupPlatforms, groupSpecies)

  protected def platformsForProbes(ps: Iterable[String]): Iterable[String] =
    ps.flatMap(platforms.platformForProbe(_)).toList.distinct

  protected def enhancedCols = true

  protected def makeMatrix(probes: Seq[String],
      typ: ValueType): ManagedMatrix = {

    val reader = try {
      if (typ == ValueType.Absolute) {
        matrixContext.absoluteDBReader
      } else {
        matrixContext.foldsDBReader
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new DBUnavailableException(e)
    }

    try {
      //Task: get rid of the enhancedCols flag
      val b = reader match {
        //Task: modify TransformingWrapper API to be able to check for type argument at runtime
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

      //Note: clarify the best code location of this heuristic
      val sparseRead = probes.size <= 10
      b.build(groups, sparseRead)(matrixContext)
    } finally {
      reader.release()
    }
  }

  lazy val managedMatrix = _managedMatrix

  def _managedMatrix: Mat = {

    val mm = if (filteredProbes.nonEmpty) {
      finish(makeMatrix(filteredProbes.toSeq, typ))
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
    val useProbes = (if (probes.nonEmpty) {
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
        if (!managedMatrix.sortColumn.contains(mc.matrixIndex) ||
            ascending != managedMatrix.sortAscending) {
          managedMatrix.sort(mc.matrixIndex, ascending)
          println(s"Sort column: ${mc.matrixIndex} ascending: $ascending")
        }
      case _ => throw new Exception("Unsupported sort method")
    }
    managedMatrix
  }

  protected def rowLabels(context: Context): RowLabels = new RowLabels(context)

  def insertAnnotations(context: Context,
      rows: Seq[ExpressionRow], withSymbols: Boolean): Seq[ExpressionRow] = {
    val rl = rowLabels(context)
    rl.insertAnnotations(rows, withSymbols)
  }
}

/**
 * A controller that produces ManagedMatrix instances and can apply a mapper.
 */
class DefaultMatrixController(context: Context, platforms: PlatformRegistry,
                              params: ControllerParams) extends MatrixController(context, platforms, params) {
  type Mat = ManagedMatrix
  def finish(mm: ManagedMatrix): ManagedMatrix = mm

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

  override def _managedMatrix: ManagedMatrix = {
    applyMapper(super._managedMatrix, mapper)
  }
}

/**
 * A matrix controller that applies the MedianValueMapper.
 */
class MergedMatrixController(context: Context,
                             platforms: PlatformRegistry,
                             params: ControllerParams, orthologs: () => Iterable[OrthologMapping])
    extends DefaultMatrixController(context, platforms, params) {

  override protected def enhancedCols = false

  lazy val orth = orthologs().filter(_.mappings.nonEmpty).headOption.getOrElse(
    throw new Exception("No ortholog mappings available, unable to create merged matrix.")
  )

  println(s"Using orthologs from ${orth.name} for mapping")

  override lazy val filteredProbes = {
    //Use orthologs to expand the probe set if this request is
    //multi-platform
    val expanded = initProbes.flatMap(orth.forProbe.getOrElse(_, Set()))
    platforms.filterProbes(expanded, groupPlatforms)
  }

  override protected def mapper: Option[MatrixMapper] = {
    val pm = new OrthologProbeMapper(orth)
    val vm = MedianValueMapper
    Some(new MatrixMapper(pm, vm))
  }

  override protected def rowLabels(context: Context) = new MergedRowLabels(context)
}
