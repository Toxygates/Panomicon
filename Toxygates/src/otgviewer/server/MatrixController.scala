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

/**
 * @author johan
 */
class MatrixController(context: Context, platforms: Platforms,
    orthologs: Iterable[OrthologMapping]) {

  private implicit val mcontext = context.matrix

  def platformsForGroups(gs: Iterable[Group]): Iterable[String] = {
    val samples = gs.toList.flatMap(_.getSamples().map(_.id))
    context.samples.platforms(samples)
  }

  def platformsForProbes(ps: Iterable[String]): Iterable[String] =
    ps.flatMap(platforms.platformForProbe(_)).toList.distinct

  def makeMatrix(requestColumns: Seq[Group], initProbes: Array[String],
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

  protected lazy val standardMapper = {
    val pm = new OrthologProbeMapper(orthologs.head)
    val vm = MedianValueMapper
    new MatrixMapper(pm, vm)
  }

  protected def groupMapper(groups: Iterable[Group]): Option[MatrixMapper] = {
    val os = groups.flatMap(_.collect("organism")).toSet
    println("Detected species in groups: " + os)
    if (os.size > 1) {
      Some(standardMapper)
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

  def loadMatrix(groups: Seq[Group], probes: Seq[String],
    typ: ValueType, syntheticColumns: Seq[Synthetic],
    mapperFromProbes: Boolean, sparseRead: Boolean,
    fullLoad: Boolean): ManagedMatrix = {
    val pt = new PerfTimer(Logger.getLogger("matrixController.loadMatrix"))

    val pfs = platformsForGroups(groups.toList)
    pt.mark("PlatformsForGroups")

    val fProbes = platforms.filterProbes(probes, pfs).toArray
    pt.mark("FilterProbes")

    val mm = makeMatrix(groups.toVector.sortBy(s => s.getName),
      fProbes, typ, sparseRead, fullLoad)
    pt.mark("MakeMatrix")

    println(mm.rawData.columnKeys.mkString(" "))

    mm.info.setPlatforms(pfs.toArray)

    val mapper = if (mapperFromProbes) {
      if (pfs.size > 1) {
        Some(standardMapper)
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

  def selectProbes(mm: ManagedMatrix, probes: Seq[String]): ManagedMatrixInfo = {
    if (!probes.isEmpty) {
      println("Refilter probes: " + probes.length)
      mm.selectProbes(probes)
    } else {
      val groups = (0 until mm.info.numDataColumns()).map(i => mm.info.columnGroup(i))
      val ps = platformsForGroups(groups)
      val allProbes = platforms.filterProbes(List(), ps).toArray
      mm.selectProbes(allProbes)
    }
    mm.info
  }

}
