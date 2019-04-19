package t.platform.mirna

import t.sparql._
import t.platform._
import t.db._
import t.platform.Species.Species
import scala.collection.immutable.DefaultMap

object TargetTable {
  def empty = (new TargetTableBuilder).build
}

sealed trait TargetSourceInfo {
  def label(score: Double): String
}

case class ScoreSourceInfo(name: String) extends TargetSourceInfo {
  def label(score: Double): String = s"$name (score: ${"%.3f".format(score)})"
}

case class SupportSourceInfo(name: String, support: String) extends TargetSourceInfo {
   def label(score: Double) = s"$name ($support)"
}

/**
 * Memory-efficient table for transcription factor targets. This contains all possible
 * interactions, respecting the user's current source selection and filtering, and is
 * the basis for network construction.
 * As much as possible we filter this table eagerly, to control the size and speed up
 * subsequent operations.
 *
 * Because the table can be large, access operations are based on traversing the entire table
 * each time data is requested. It is thus more efficient to request interactions for a set of
 * probes than to ask for a few probes at a time.
 *
 * Convention: origins are miRNAs such as hsa-let-7a-2-3p,
 * targets are mRNAs (identified by refSeq transcripts
 * such as NM_133594)
 */
class TargetTable(
  val origins: Array[String],
  val targets: Array[String],
  val scores: Array[Double],
  val info: Array[TargetSourceInfo]) extends IndexedSeq[Interaction] {

  override val length: Int = origins.length

  def apply(i: Int) = (MiRNA(origins(i)), RefSeq(targets(i)), scores(i), info(i))

  def filterWith(test: Int => Boolean): TargetTable = {
    val builder = new TargetTableBuilder()
    for {
      i <- 0 until size;
      if test(i)
    } builder.add(MiRNA(origins(i)), RefSeq(targets(i)), scores(i), info(i))

    builder.build
  }

  def scoreFilter(minScore: Double): TargetTable =
    filterWith(scores(_) >= minScore)

  def speciesFilter(species: Species): TargetTable = {
    val shortCode = species.shortCode
    filterWith(origins(_).startsWith(s"${shortCode}-"))
  }

  /**
   * Find probes in the platform that match the given transcripts.
   */
  def probesForTranscripts(platform: Iterable[Probe], transcripts: Iterable[RefSeq]):
    Iterable[(RefSeq, Iterable[Probe])] = {
    // Note: this function could be a static lookup map?
    val allTrn = transcripts.toSet
    val r = for {
      p <- platform.toSeq;
      foundTrn <- p.transcripts.filter(allTrn.contains)
    } yield (foundTrn, p)
    r.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
   * miRNA to mRNA lookup without a platform. Simple RefSeq IDs will be returned.
   */
  def targets(miRNAs: Iterable[MiRNA]): Iterable[(MiRNA, RefSeq, Double, String)] = {
    val allMicro = miRNAs.toSet
    for {
      (origin, target, score, info) <- this;
      if (allMicro.contains(origin))
    } yield (origin, target, score, info.label(score))
  }

  /**
   * Efficient miRNA to mRNA lookup for a specific mRNA platform.
   * mRNA probes in the platform must have transcripts populated.
   */
  def targets(miRNAs: Iterable[MiRNA], platform: Iterable[Probe]): Iterable[(MiRNA, Probe, Double, String)] = {
    val allTrn = targets(miRNAs)
    val probeLookup = Map() ++ probesForTranscripts(platform, allTrn.map(_._2))
    allTrn.flatMap(x => probeLookup.get(x._2) match {
      case Some(ps) => ps.map((x._1, _, x._3, x._4))
      case _        => Seq()
    })
  }

  /**
   * Efficient mRNA to miRNA lookup.
   * Probes must have transcripts populated.
   */
  def reverseTargets(mRNAs: Iterable[Probe]): Iterable[(Probe, MiRNA, Double, String)] = {
    val allTrns = mRNAs.flatMap(p => p.transcripts.map(tr => (tr, p)))
    val allTrLookup = allTrns.groupBy(_._1)
    val allTrKeys = allTrLookup.keySet
    println(s"Size ${allTrKeys.size} transcript key set")
    for {
      (origin, target, score, info) <- this;
      if allTrKeys contains target;
      (refSeq, probe) <- allTrLookup(target)
    } yield (probe, origin, score, info.label(score))
  }

  final def limitSize[T](data: Iterable[T], limit: Option[Int]) = limit match {
    case Some(n) => data take n
    case None    => data
  }

  /**
   * Convenience method.
   * If not from MiRNA, then probes must have transcripts populated.
   * If from MiRNA, then any platform given must have transcripts populated.
   */
  def associationLookup(
    probes: Seq[Probe],
    fromMirna: Boolean,
    platform: Option[Iterable[Probe]],
    sizeLimit: Option[Int] = None): MMap[Probe, DefaultBio] = {
    if (fromMirna) {
      val targetRes = platform match {
        //Return probes in the requested mRNA platform
        case Some(p) => targets(probes.map(p => MiRNA(p.identifier)), p).map(x =>
          (x._1.asProbe, DefaultBio(x._2.identifier, x._2.identifier, Some(x._4))))
        //Return plain RefSeq IDs
        case None => targets(probes.map(p => MiRNA(p.identifier))).map(x =>
          (x._1.asProbe, DefaultBio(x._2.id, x._2.id, Some(x._4))))
      }

      makeMultiMap(limitSize(targetRes,sizeLimit))
    } else {
      makeMultiMap(limitSize(reverseTargets(probes), sizeLimit).map(x =>
        (x._1, DefaultBio(x._2.id, x._2.id, Some(x._4)))))
    }
  }
}

class TargetTableBuilder {
  var origins = List[String]()
  var targets = List[String]()
  var scores = List[Double]()
  var infos = List[TargetSourceInfo]()

  def add(origin: MiRNA, target: RefSeq, score: Double,
    info: TargetSourceInfo) {
    origins ::= origin.id
    targets ::= target.id
    scores ::= score
    infos ::= info
  }

  def addAll(other: TargetTable) {
    for ((o, t, sc, info) <- other) {
      add(o, t, sc, info)
    }
  }

  def build =
    new TargetTable(origins.toArray, targets.toArray, scores.toArray, infos.toArray)
}
