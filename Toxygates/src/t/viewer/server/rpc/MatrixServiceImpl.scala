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

package t.viewer.server.rpc

import java.util.ArrayList
import java.util.{ List => JList }
import t.viewer.server.EVArray
import t.common.shared.sample.ExprMatrix
import otgviewer.server.ExtFoldBuilder
import otgviewer.server.FoldBuilder
import otgviewer.server.ManagedMatrix
import otgviewer.server.ManagedMatrixBuilder
import otgviewer.server.MatrixMapper
import otgviewer.server.NormalizedBuilder
import otgviewer.server.rpc.Conversions
import otgviewer.server.rpc.Conversions.asScala
import otgviewer.shared.DBUnavailableException
import otgviewer.shared.FullMatrix
import otgviewer.shared.Group
import otgviewer.shared.ManagedMatrixInfo
import otgviewer.shared.NoDataLoadedException
import otgviewer.shared.Synthetic
import t.BaseConfig
import t.Context
import t.common.shared.AType
import t.common.shared.DataSchema
import t.common.shared.ValueType
import t.common.shared.probe.MedianValueMapper
import t.common.shared.probe.OrthologProbeMapper
import t.common.shared.sample.ExpressionRow
import t.common.shared.sample.ExpressionValue
import t.db.MatrixContext
import t.db.MatrixDBReader
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.kyotocabinet.KCMatrixDB
import t.platform.OrthologMapping
import t.platform.Probe
import t.sparql._
import t.viewer.client.rpc.MatrixService
import t.viewer.server.CSVHelper
import t.viewer.server.Configuration
import t.viewer.server.Feedback
import t.viewer.server.Platforms
import t.viewer.shared.table.SortKey
import t.common.server.ScalaUtils
import t.common.shared.PerfTimer
import java.util.logging.Logger
import otgviewer.shared.OTGSample
import otgviewer.server.MatrixController
import javax.annotation.Nullable

object MatrixServiceImpl {

  /**
   * Note that in some cases, using the ServletContext rather than
   * static vars is more robust.
   */

  var inited = false
  var orthologs: Iterable[OrthologMapping] = _

  def staticInit(c: Context) = synchronized {
    if (!inited) {
      val probes = c.probes
      orthologs = probes.orthologMappings
      inited = true
    }
  }
}

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 *
 * TODO move dependencies from otgviewer to t.common
 */
abstract class MatrixServiceImpl extends TServiceServlet with MatrixService {
  import Conversions._
  import scala.collection.JavaConversions._
  import ScalaUtils._
  import MatrixServiceImpl._

  protected implicit var mcontext: MatrixContext = _
  private def probes = context.probes
  private var config: Configuration = _
  private val logger = Logger.getLogger("MatrixService")

  // Useful for testing
  override def localInit(config: Configuration) {
    super.localInit(config)
    this.config = config
    mcontext = context.matrix
    staticInit(context)
  }

  protected class MatrixState {
    var controller: MatrixController = _
    def matrix: ManagedMatrix = controller.managedMatrix
  }

  def getSessionData(): MatrixState = {
    val session = getThreadLocalRequest.getSession
    val r = Option(session.getAttribute("matrix").
      asInstanceOf[MatrixState])
    r match {
      case Some(ms) => ms
      case None =>
        val r = new MatrixState()
        session.setAttribute("matrix", r)
        r
    }
  }

  def setSessionData(m: MatrixState) =
    getThreadLocalRequest().getSession().setAttribute("matrix", m)

  def identifiersToProbes(identifiers: Array[String], precise: Boolean,
    titlePatternMatch: Boolean, samples: JList[OTGSample]): Array[String] = {
    val ps = if (titlePatternMatch) {
      probes.forTitlePatterns(identifiers)
    } else {
      probes.identifiersToProbes(mcontext.probeMap,
        identifiers, precise)
    }
    val result = ps.map(_.identifier).toArray

    Option(samples) match {
      case Some(_) => filterProbesByGroup(result, samples)
      case None    => result
    }
  }

  // TODO Shared logic with SparqlService
  def filterProbesByGroup(ps: Array[String], samples: JList[OTGSample]): Array[String] = {
    val platforms: Set[String] = samples.map(x => x.get("platform_id")).toSet
    val lookup = probes.platformsAndProbes
    val acceptProbes = platforms.flatMap(p => lookup(p))

    ps.filter(x => acceptProbes.contains(x))
  }

  def loadMatrix(groups: JList[Group], probes: Array[String],
    typ: ValueType): ManagedMatrixInfo = {
    getSessionData.controller =
      new MatrixController(context, orthologs, groups, probes, typ, false, false, false)
    getSessionData.matrix.info
  }

  @throws(classOf[NoDataLoadedException])
  def selectProbes(@Nullable probes: Array[String]): ManagedMatrixInfo = {
    val prs = Option(probes).getOrElse(Array()).toSeq
    getSessionData.controller.selectProbes(prs).info
  }

  @throws(classOf[NoDataLoadedException])
  def setColumnThreshold(column: Int, threshold: java.lang.Double): ManagedMatrixInfo = {
    val mm = getSessionData.matrix
    println(s"Filter column $column at $threshold")
    mm.setFilterThreshold(column, threshold)
    mm.info
  }

  @throws(classOf[NoDataLoadedException])
  def matrixRows(offset: Int, size: Int, sortKey: SortKey,
    ascending: Boolean): JList[ExpressionRow] = {
    val mm =
      getSessionData.controller.applySorting(sortKey, ascending)

    val mergeMode = mm.info.getPlatforms.size > 1

    new ArrayList[ExpressionRow](
      insertAnnotations(mm.current.asRows.drop(offset).take(size), mergeMode))
  }

  //this is probably quite inefficient
  private def withCount[T](xs: Seq[T]): Iterable[(T, Int)] =
    xs.distinct.map(x => (x, xs.count(_ == x)))

  private def prbCount(n: Int) = {
    if (n == 0) {
      "No probes"
    } else if (n == 1) {
      "1 probe"
    } else {
      s"$n probes"
    }
  }

  private def repeatStrings[T](xs: Seq[T]): Iterable[String] =
    withCount(xs).map(x => s"${x._1} (${prbCount(x._2)})")

  /**
   * Dynamically obtain annotations such as probe titles, gene IDs and gene symbols,
   * appending them to the rows just before sending them back to the client.
   * Unsuitable for large amounts of data.
   */
  private def insertAnnotations(rows: Seq[ExpressionRow], mergeMode: Boolean): Seq[ExpressionRow] = {
    val allAtomics = rows.flatMap(_.getAtomicProbes)

    val attribs = probes.withAttributes(allAtomics.map(Probe(_)))
    val pm = Map() ++ attribs.map(a => (a.identifier -> a))
    println(pm.take(5))

    rows.map(or => {
      val atomics = or.getAtomicProbes()
      val ps = atomics.flatMap(pm.get(_))

      if (mergeMode) {
        val expandedGenes = ps.flatMap(p =>
          p.genes.map(g => (schema.platformSpecies(p.platform), g.identifier)))
        val expandedSymbols = ps.flatMap(p =>
          p.symbols.map(schema.platformSpecies(p.platform) + ":" + _.symbol))

        val r = new ExpressionRow(atomics.mkString("/"),
          atomics,
          repeatStrings(ps.map(p => p.name)).toArray,
          expandedGenes.map(_._2).distinct,
          repeatStrings(expandedSymbols).toArray,
          or.getValues)

        val gils = withCount(expandedGenes).map(x => s"${x._1._1 + ":" + x._1._2} (${prbCount(x._2)})").toArray
        r.setGeneIdLabels(gils)
        r
      } else {
        assert(ps.size == 1)
        val p = atomics(0)
        val pr = pm.get(p)
        new ExpressionRow(p,
          pr.map(_.name).getOrElse(""),
          pr.toArray.flatMap(_.genes.map(_.identifier)),
          pr.toArray.flatMap(_.symbols.map(_.symbol)),
          or.getValues)
      }
    })
  }

  def getFullData(gs: JList[Group], rprobes: Array[String], sparseRead: Boolean,
    withSymbols: Boolean, typ: ValueType): FullMatrix = {
    val sgs = Vector() ++ gs
    val controller = new MatrixController(context, orthologs, gs, rprobes,
        typ, true, sparseRead, true)
    val mm = controller.managedMatrix

    val raw = if (sgs.size == 1) {
      //break out each individual sample if it's only one group
      mm.rawData.selectNamedColumns(sgs(0).getSamples().map(_.id())).asRows
    } else {
      mm.current.selectNamedColumns(sgs.map(_.getName)).asRows
    }

    val rows = if (withSymbols) {
      insertAnnotations(raw, controller.groupPlatforms.size > 1)
    } else {
      val ps = raw.flatMap(or => or.getAtomicProbes.map(Probe(_)))
      val ats = probes.withAttributes(ps)
      val giMap = Map() ++ ats.map(x =>
        (x.identifier -> x.genes.map(_.identifier).toArray))

      //TODO: some clients need neither "symbols"/annotations nor geneIds
      raw.map(or => {
        new ExpressionRow(or.getProbe, or.getAtomicProbes, or.getAtomicProbeTitles,
          or.getAtomicProbes.flatMap(p => giMap(p)),
          or.getGeneSyms, or.getValues)
      })
    }
    new FullMatrix(mm.info, new ArrayList[ExpressionRow](rows))
  }

  @throws(classOf[NoDataLoadedException])
  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): Unit =
    getSessionData.matrix.addSynthetic(test)

  @throws(classOf[NoDataLoadedException])
  def removeTwoGroupTests(): Unit =
    getSessionData.matrix.removeSynthetics

    @throws(classOf[NoDataLoadedException])
    def prepareCSVDownload(individualSamples: Boolean): String = {
      val mm = getSessionData.matrix
      var mat = if (individualSamples &&
        mm.rawUngroupedMat != null && mm.current != null) {
        //Individual samples
        val info = mm.info
        val ug = mm.rawUngroupedMat.selectNamedRows(mm.current.rowKeys.toSeq)
        val parts = (0 until info.numDataColumns).map(g => {
          if (!info.isPValueColumn(g)) {
            //Sample data.
            //Help the user by renaming the columns.
            //Prefix sample IDs by group IDs.

            //Here we get both treated and control samples from cg, but
            //except for single unit columns in the normalized intensity case,
            // only treated will be present in ug.
            val ids = info.samples(g).map(_.id)
            val sel = ug.selectNamedColumns(ids)
            val newNames = Map() ++ sel.columnMap.map(x => (info.columnName(g) + ":" + x._1 -> x._2))
            sel.copyWith(sel.data, sel.rowMap, newNames)
          } else {
            //p-value column, present as it is
            mm.current.selectNamedColumns(List(info.columnName(g)))
          }
        })

        parts.reduce(_ adjoinRight _)
      } else {
        //Grouped
        mm.current
      }


    val colNames = mat.sortedColumnMap.map(_._1)
    val rows = mat.asRows
    //TODO shared logic with e.g. insertAnnotations, extract
    val rowNames = rows.map(_.getAtomicProbes.mkString("/"))

    //May be slow!
    val gis = probes.allGeneIds.mapInnerValues(_.identifier)
    val atomics = rows.map(_.getAtomicProbes())
    val geneIds = atomics.map(row =>
      row.flatMap(at => gis.getOrElse(Probe(at), Seq.empty))).map(_.distinct.mkString(" "))

    val aux = List((("Gene"), geneIds))
    CSVHelper.writeCSV(config.csvDirectory, config.csvUrlBase,
      aux ++ csvAuxColumns(mat),
      rowNames, colNames,
      mat.data.map(_.map(asScala(_))))
  }

  protected def csvAuxColumns(mat: ExprMatrix): Seq[(String, Seq[String])] = Seq()

  @throws(classOf[NoDataLoadedException])
  def getGenes(limit: Int): Array[String] = {
    val mm = getSessionData().matrix

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
    val mm = getSessionData.matrix
    var state = "(No user state available)"
    if (mm != null && mm.current != null) {
      state = "Matrix: " + mm.current.rowKeys.size + " x " + mm.current.columnKeys.size
      state += "\nColumns: " + mm.current.columnKeys.mkString(", ")
    }
    Feedback.send(name, email, feedback, state, config.feedbackReceivers,
      config.feedbackFromAddress, context.config.appName)
  }
}
