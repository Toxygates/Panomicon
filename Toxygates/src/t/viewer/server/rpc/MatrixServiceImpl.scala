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
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList
import otgviewer.server.EVArray
import otgviewer.server.ExprMatrix
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
import otgviewer.server.ScalaUtils
import t.common.shared.PerfTimer
import java.util.logging.Logger
import otgviewer.shared.OTGSample

object MatrixServiceImpl {

  /**
   * Note that in some cases, using the ServletContext rather than
   * static vars is more robust.
   */

  var inited = false

  //This in particular could be put in a servlet context and shared between
  //servlets.
  var platforms: Platforms = _
  var orthologs: Iterable[OrthologMapping] = _

  def staticInit(c: Context) = synchronized {
    if (!inited) {
      val probes = c.probes
      orthologs = probes.orthologMappings
      platforms = Platforms(probes)
      inited = true
    }
  }
}

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 *
 * This is currently the only servlet that (explicitly)
 * maintains server side sessions.
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
    var matrix: ManagedMatrix = _
    var sortAssoc: AType = _
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

  //Should this be in sparqlService?
  def identifiersToProbes(identifiers: Array[String], precise: Boolean,
    titlePatternMatch: Boolean): Array[String] = {
    identifiersToProbes(identifiers, precise, titlePatternMatch, null)
  }

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

  // TODO Same codes in SparqlService
  def filterProbesByGroup(ps: Array[String], samples: JList[OTGSample]): Array[String] = {
    val platforms: Set[String] = samples.map(x => x.get("platform_id")).toSet
    val lookup = probes.platformsAndProbes
    val acceptProbes = platforms.flatMap(p => lookup(p))

    ps.filter(x => acceptProbes.contains(x))
  }

  private def makeMatrix(requestColumns: Seq[Group], initProbes: Array[String],
    typ: ValueType, sparseRead: Boolean = false, fullLoad: Boolean = false): ManagedMatrix = {

    val reader = try {
      if (typ == ValueType.Absolute) {
        mcontext.absoluteDBReader
      } else {
        mcontext.foldsDBReader
      }
    } catch {
      case e: Exception => throw new DBUnavailableException()
    }

    val pfs = platformsForGroups(requestColumns)
    val multiPlat = pfs.size > 1

    try {
      val enhancedCols = !multiPlat

      val b = reader match {
        case ext: KCExtMatrixDB =>
          assert(typ == ValueType.Folds)
          new ExtFoldBuilder(enhancedCols, ext, initProbes)
        case db: KCMatrixDB =>
          if (typ == ValueType.Absolute) {
            new NormalizedBuilder(enhancedCols, db, initProbes)
          } else {
            new FoldBuilder(db, initProbes)
          }
        case _ => throw new Exception("Unexpected DB reader type")
      }
      b.build(requestColumns, sparseRead, fullLoad)
    } finally {
      reader.release
    }
  }

  private def platformsForGroups(gs: Iterable[Group]): Iterable[String] = {
    val samples = gs.toList.flatMap(_.getSamples().map(_.id))
    context.samples.platforms(samples)
  }

  private def platformsForProbes(ps: Iterable[String]): Iterable[String] =
    ps.flatMap(platforms.platformForProbe(_))

  private def applyMapper(groups: JList[Group], mm: ManagedMatrix): ManagedMatrix = {
    mapper(groups) match {
      case Some(m) => m.convert(mm)
      case None    => mm
    }
  }

  def loadMatrix(groups: JList[Group], probes: Array[String],
    typ: ValueType, syntheticColumns: JList[Synthetic]): ManagedMatrixInfo = {
    val pt = new PerfTimer(Logger.getLogger("matrixService.loadMatrix"))

    val pfs = platformsForGroups(groups.toList)
    pt.mark("PlatformsForGroups")

    val fProbes = platforms.filterProbes(probes, pfs).toArray
    pt.mark("FilterProbes")

    val mm = makeMatrix(groups.toVector, fProbes, typ)
    pt.mark("MakeMatrix")

    mm.info.setPlatforms(pfs.toArray)
    //    selectProbes(probes)
    val mm2 = applyMapper(groups, mm)
    pt.mark("ApplyMapper")

    getSessionData.matrix = mm2

    pt.finish()

    mm2.info
  }

  @throws(classOf[NoDataLoadedException])
  def selectProbes(probes: Array[String]): ManagedMatrixInfo = {
    if (probes != null) {
      println("Refilter probes: " + probes.length)
    }
    val mm = getSessionData.matrix

    //    mm.filterData(Some(absValFilter))
    if (probes != null && probes.length > 0) {
      mm.selectProbes(probes)
    } else {
      val groups = (0 until mm.info.numDataColumns()).map(i => mm.info.columnGroup(i))
      val ps = platformsForGroups(groups)
      val allProbes = platforms.filterProbes(List(), ps).toArray
      mm.selectProbes(allProbes)
    }
    mm.info
  }

  @throws(classOf[NoDataLoadedException])
  def setColumnThreshold(column: Int, threshold: java.lang.Double): ManagedMatrixInfo = {
    val mm = getSessionData.matrix
    println(s"Filter column $column at $threshold")
    mm.setFilterThreshold(column, threshold)
    mm.info
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

  @throws(classOf[NoDataLoadedException])
  def matrixRows(offset: Int, size: Int, sortKey: SortKey,
    ascending: Boolean): JList[ExpressionRow] = {
    val pt = new PerfTimer(Logger.getLogger("matrixService.matrixRows"))

    val session = getSessionData.matrix

    sortKey match {
      case mc: SortKey.MatrixColumn =>
        if (Some(mc.matrixIndex) != session.sortColumn ||
          ascending != session.sortAscending) {
          session.sort(mc.matrixIndex, ascending)
          println("SortCol: " + mc.matrixIndex + " asc: " + ascending)
        }
      case as: SortKey.Association =>
        val old = getSessionData.sortAssoc
        val nw = as.atype
        if (old == null || nw != old
          || ascending != session.sortAscending) {
          getSessionData.sortAssoc = nw
          val st = assocSortTable(as.atype,
            getSessionData.matrix.probesForAuxTable)
          session.sortWithAuxTable(st, ascending)
          println("Sort with aux table for " + as)
        }
    }
    pt.mark("Sort")

    val mm = session.current
    val mergeMode = session.info.getPlatforms.size > 1

    val r = new ArrayList[ExpressionRow](
      insertAnnotations(mm.asRows.drop(offset).take(size), mergeMode))
    pt.mark("insertAnnotations")
    pt.finish()
    r
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

  //TODO simplify
  def getFullData(gs: JList[Group], rprobes: Array[String], sparseRead: Boolean,
    withSymbols: Boolean, typ: ValueType): FullMatrix = {
    val sgs = Vector() ++ gs
    val pfs = platformsForGroups(sgs)

    val realProbes = platforms.filterProbes(rprobes, pfs).toArray
    val mm = makeMatrix(sgs, realProbes.toArray, typ, sparseRead, true)
    mm.info.setPlatforms(pfs.toArray)

    val mapper = mapperForProbes(realProbes)
    val mm2 = mapper.map(mx => mx.convert(mm)).getOrElse(mm)

    val raw = if (sgs.size == 1) {
      //break out each individual sample
      mm2.rawData.selectNamedColumns(sgs(0).getSamples().map(_.id())).asRows
    } else {
      mm2.current.selectNamedColumns(sgs.map(_.getName)).asRows
    }

    val rows = if (withSymbols) {
      insertAnnotations(raw, pfs.size > 1)
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
    new FullMatrix(mm2.info, new ArrayList[ExpressionRow](rows))
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
      val info = mm.info
      val ug = mm.rawUngroupedMat.selectNamedRows(mm.current.rowKeys.toSeq)
      val gs = (0 until info.numDataColumns()).map(g => info.columnGroup(g))
      //Help the user by renaming the columns.
      //Prefix sample IDs by group IDs.
      val parts = gs.map(g => {
        val ids = g.getTreatedSamples.map(_.id)
        val sel = ug.selectNamedColumns(ids)
        val newNames = Map() ++ sel.columnMap.map(x => (g.getName + ":" + x._1 -> x._2))
        sel.copyWith(sel.data, sel.rowMap, newNames)
      })
      parts.reduce(_ adjoinRight _)
    } else {
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

  private lazy val standardMapper = {
    val pm = new OrthologProbeMapper(orthologs.head)
    val vm = MedianValueMapper
    new MatrixMapper(pm, vm)
  }

  protected def mapper(groups: Iterable[Group]): Option[MatrixMapper] = {
    val os = groups.flatMap(_.collect("organism")).toSet
    println("Detected species in groups: " + os)
    if (os.size > 1) {
      Some(standardMapper)
    } else {
      None
    }
  }

  protected def mapperForProbes(ps: Iterable[String]): Option[MatrixMapper] = {
    val pfs = platformsForProbes(ps).toSet
    if (pfs.size > 1) {
      Some(standardMapper)
    } else {
      None
    }
  }
}
