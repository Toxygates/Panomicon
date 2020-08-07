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

package t.viewer.server.rpc

import java.util.logging.Logger
import java.util.{List => JList}

import javax.annotation.Nullable
import t.Context
import t.clustering.server.RClustering
import t.clustering.shared.Algorithm
import t.common.shared.ValueType
import t.common.shared.sample.{ExpressionRow, Group}
import t.db.MatrixContext
import t.platform.{OrthologMapping, Probe}
import t.viewer.client.rpc.{MatrixService, NetworkService}
import t.viewer.server._
import t.viewer.server.matrix._
import t.viewer.shared._

object MatrixServiceImpl {

  private var orthologs: Option[Iterable[OrthologMapping]] = None

  def getOrthologs(c: Context) = synchronized {
    if (orthologs == None) {
      val probes = c.probeStore
      orthologs = Some(probes.orthologMappings)
    }
    orthologs.get
  }
}

object MatrixState {
  def stateKey = "matrix"
}

/**
 * The MatrixState tracks a number of managed matrices, each identified by a
 * unique ID string.
 */
class MatrixState {
  var controllers: Map[String, MatrixController] = Map()

  def controller(id: String) =
    controllers.getOrElse(id, throw new NoDataLoadedException)

  def matrix(id: String): MatrixController#Mat =
    controller(id).managedMatrix

  def matrixOption(id: String): Option[MatrixController#Mat] =
    controllers.get(id).map(_.managedMatrix)

  /**
   * Given the requested groups and value type, is it necessary to reload
   * the given matrix?
   */
  def needsReload(id: String, groups: Iterable[Group], typ: ValueType): Boolean = {
    if (id == null) {
      return true
    }
    val cont = controllers.get(id)
    cont == None ||
      cont.get.groups.toSet != groups.toSet ||
      cont.get.typ != typ
  }
}

/**
 * This servlet is responsible for obtaining and manipulating matrices
 * with gene expression data.
 */
class MatrixServiceImpl extends StatefulServlet[MatrixState] with MatrixService {
  import MatrixServiceImpl._
  import t.common.server.GWTUtils._

  import scala.collection.JavaConverters._

  protected implicit var mcontext: MatrixContext = _
  private def probes = context.probeStore
  private var config: Configuration = _
  private val logger = Logger.getLogger("MatrixService")
  private var codeDir: String = null

  // Useful for testing
  override def localInit(config: Configuration) {
    super.localInit(config)
    this.config = config
    this.codeDir = this.getServletContext.getRealPath("/WEB-INF/")
    mcontext = context.matrix
  }

  protected def stateKey = MatrixState.stateKey
  protected def newState = new MatrixState

  def loadMatrix(id: String, groups: JList[Group], probes: Array[String],
    typ: ValueType,
    initFilters: JList[ColumnFilter]): ManagedMatrixInfo = {

    val allProbes = Seq()
    //Always load the empty probe set(all probes), to be able to revert to this view
    //later.
    getState.controllers += (id ->
      MatrixController(context, () => getOrthologs(context),
          groups.asScala, allProbes, typ))
    val mat = getState.matrix(id)

    if (!probes.isEmpty) {
      mat.selectProbesAndFilter(probes, initFilters.asScala)
    } else if (!initFilters.isEmpty) {
      mat.setFilters(initFilters.asScala)
    }
    mat.info
  }

  /**
   * Obtain MatrixState (or NetworkState) for the named matrix.
   */
  protected def stateFor(id: String) = {
    val r = if (id.startsWith(NetworkService.tablePrefix)) {
      getOtherServiceState[NetworkState](NetworkState.stateKey)
    } else {
      Some(getState)
    }
    r.getOrElse(throw new NoDataLoadedException(s"No matrix state for id $id"))
  }

  @throws(classOf[NoDataLoadedException])
  def selectProbes(id: String, @Nullable probes: Array[String]): ManagedMatrixInfo = {
    val prs = Option(probes).getOrElse(Array()).toSeq
    stateFor(id).controller(id).selectProbes(prs).info
  }

  @throws(classOf[NoDataLoadedException])
  def setColumnFilter(id: String, column: Int, f: ColumnFilter): ManagedMatrixInfo = {
    val mm = stateFor(id).matrix(id)

    println(s"Filter for column $column: $f")
    mm.setFilter(column, f)
    mm.info
  }

  @throws(classOf[NoDataLoadedException])
  def clearColumnFilters(id: String, columns: Array[Int]): ManagedMatrixInfo = {
    val mm = stateFor(id).matrix(id)

    println(s"Clear filter for columns $columns")
    columns.foreach(mm.setFilter(_, ColumnFilter.emptyLT));
    mm.info
  }

  @throws(classOf[NoDataLoadedException])
  def matrixRows(id: String, offset: Int, size: Int, sortKey: SortKey,
    ascending: Boolean): JList[ExpressionRow] = {
    val cont = stateFor(id).controller(id)
    val mm = cont.applySorting(sortKey, ascending)

    val grouped = mm.getPageView(offset, size)
    val rowNames = grouped.map(_.getProbe)
    val rawData = mm.rawUngrouped.selectNamedRows(rowNames).rowData

    for (
      (gr, rr) <- grouped zip rawData;
      (gv, i) <- gr.getValues.zipWithIndex
    ) {
      val tooltip = if (mm.info.isPValueColumn(i)) {
        "p-value (t-test treated against control)"
      } else {
        val basis = mm.baseColumns(i)
        val rawRow = basis.map(i => rr(i))
        ManagedMatrix.makeTooltip(rawRow)
      }
      gv.setTooltip(tooltip)
    }
    cont.insertAnnotations(context, schema, grouped, true).asGWT
  }

  def getFullData(gs: JList[Group], rprobes: Array[String],
    withSymbols: Boolean, typ: ValueType): FullMatrix = {
    val groups = Vector() ++ gs.asScala
    val controller = MatrixController(context, () => getOrthologs(context),
        groups, rprobes, typ)
    val mm = controller.managedMatrix

    val raw = if (groups.size == 1) {
      //break out each individual sample if it's only one group
      val samples = groups(0).getSamples().map(_.id)
      mm.rawUngrouped.selectNamedColumns(samples).asRows
    } else {
      val cols = groups.map(_.getName)
      mm.current.selectNamedColumns(cols).asRows
    }

    val rows =
      controller.insertAnnotations(context, schema, raw, withSymbols)
    new FullMatrix(mm.info, rows.asGWT)
  }

  @throws(classOf[NoDataLoadedException])
  def addSyntheticColumn(id: String, synth: Synthetic): ManagedMatrixInfo = {
    val current = stateFor(id).matrix(id)
    current.addSynthetic(synth)
    current.info
  }

  @throws(classOf[NoDataLoadedException])
  def removeSyntheticColumns(id: String): ManagedMatrixInfo = {
    val current = stateFor(id).matrix(id)
    current.removeSynthetics()
    current.info
  }

  @throws(classOf[NoDataLoadedException])
  def prepareCSVDownload(id: String, individualSamples: Boolean): String = {
    val managedMat = stateFor(id).matrix(id)
    config.csvUrlBase + "/" +
      CSVDownload.generate(managedMat, probes, config.csvDirectory, individualSamples)
  }

  @throws(classOf[NoDataLoadedException])
  def getGenes(id: String, limit: Int): Array[String] = {
    val mm = stateFor(id).matrix(id)

    var rowNames = mm.current.sortedRowMap.map(_._1)
    println(rowNames.take(10))
    if (limit != -1) {
      rowNames = rowNames.take(limit)
    }

    val gis = probes.allGeneIds()
    val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
    geneIds.flatten.map(_.identifier).toArray
  }

  def sendFeedback(name: String, email: String, feedback: String): Unit = {
    val mm = getState.controllers.headOption.map(_._2.managedMatrix)
    var state = "(No user state available)"
    if (mm != None && mm.get.current != null) {
      val cmat = mm.get.current
      state = "Matrix: " + cmat.rowKeys.size + " x " + cmat.columnKeys.size
      state += "\nColumns: " + cmat.columnKeys.mkString(", ")
    }
    Feedback.send(name, email, feedback, state, config.feedbackReceivers,
      config.feedbackFromAddress, context.config.appName)
  }

  //Note, this is currently unsupported for NetworkService matrices
  @throws(classOf[NoDataLoadedException])
  def prepareHeatmap(id: String, groups: JList[Group], chosenProbes: JList[String],
    algorithm: Algorithm, featureDecimalDigits: Int): String = {
    prepareHeatmap(id, groups, chosenProbes, ValueType.Folds, algorithm,
        featureDecimalDigits)
  }

  @throws(classOf[NoDataLoadedException])
  def prepareHeatmap(id: String, groups: JList[Group], chosenProbes: JList[String],
    valueType: ValueType, algorithm: Algorithm, featureDecimalDigits: Int): String = {
    val probesScala = chosenProbes.asScala

    //Reload data in a temporary controller if groups do not correspond to
    //the ones in the current session
    val cont = if (getState.needsReload(id, groups.asScala, valueType)) {
      MatrixController(context, () => getOrthologs(context),
          groups.asScala, probesScala, valueType)
    } else {
      getState.controller(id)
    }

    val data = new ClusteringData(cont, probes, probesScala, valueType)

    // R can't deal with backslashes in a file path so we need to replace them
    // with slashes
    val clust = new RClustering(codeDir.replace("\\", "/"))
    clust.clustering(data.data.flatten, rowNamesForHeatmap(data.rowNames),
        data.colNames, data.geneSymbols, algorithm, featureDecimalDigits)
  }

  protected def rowNamesForHeatmap(names: Array[String]): Array[String] =
    names

}
