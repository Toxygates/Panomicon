package t.platform.mirna

import t.sparql._
import t.platform._
import t.db._
import t.platform.Species.Species
import scala.collection.immutable.DefaultMap

object TargetTable {
  def interactionLabel(intn: (Any, Any, Double, String)) =
    s"${intn._4} (score: ${"%.3f".format(intn._3)})"

  def empty = (new TargetTableBuilder).build
}

/**
 * Memory-efficient table for transcription factor targets.
 *
 * Convention: origins are miRNAs such as hsa-let-7a-2-3p,
 * targets are mRNAs (identified by refSeq transcripts
 * such as NM_133594)
 *
 * The database array identifies the source of a particular origin-target pair.
 */
class TargetTable(val origins: Array[String],
    val targets: Array[String],
    val scores: Array[Double],
    val database: Array[String]) extends IndexedSeq[Interaction] {

  override val length: Int = origins.length

  def apply(i: Int) = (MiRNA(origins(i)), RefSeq(targets(i)), scores(i), database(i))

  def filterWith(test: Int => Boolean): TargetTable = {
    val builder = new TargetTableBuilder
    for {
      i <- 0 until size;
      if test(i)
    } builder.add(MiRNA(origins(i)), RefSeq(targets(i)), scores(i), database(i))

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
  def probesForTranscripts(platform: Iterable[Probe], transcripts: Iterable[RefSeq]): Iterable[(RefSeq, Iterable[Probe])] = {
    // Note: this function could be a static lookup map?
    val allTrn = transcripts.toSet
    val r = for {
      p <- platform.toSeq;
      foundTrn <- p.transcripts.filter(allTrn.contains)
    } yield (foundTrn, p)
    r.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
   * Efficient miRNA to mRNA lookup.
   * mRNA probes in the platform must have transcripts populated.
   */
  def targets(miRNAs: Iterable[MiRNA], platform: Iterable[Probe]): Iterable[(MiRNA, Probe, Double, String)] = {
    val allMicro = miRNAs.toSet
    val allTrn = for {
      (origin, target, score, db) <- this;
      if (allMicro.contains(origin))
    } yield (origin, target, score, db)
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
      (origin, target, score, db) <- this;
      if allTrKeys contains target;
      (refSeq, probe) <- allTrLookup(target)
    } yield (probe, origin, score, db)
  }

  final def limitSize[T](data: Iterable[T], limit: Option[Int]) = limit match {
    case Some(n) => data take n
    case None    => data
  }

  final def label(x: (Any, Any, Double, String)) = TargetTable.interactionLabel(x)

  /**
   * Convenience method.
   * If not from MiRNA, then probes must have transcripts populated.
   * If from MiRNA, then the platform must have transcripts populated.
   */
  def associationLookup(probes: Seq[Probe],
    fromMirna: Boolean,
    platform: Iterable[Probe],
    sizeLimit: Option[Int] = None): MMap[Probe, DefaultBio] = {
    if (fromMirna) {
      makeMultiMap(
        limitSize(
          targets(probes.map(p => MiRNA(p.identifier)), platform),
          sizeLimit).map(x =>
            (x._1.asProbe, DefaultBio(x._2.identifier, x._2.identifier, Some(label(x))))))
    } else {
      makeMultiMap(limitSize(reverseTargets(probes), sizeLimit).map(x =>
        (x._1, DefaultBio(x._2.id, x._2.id, Some(label(x))))))
    }
  }
}

class TargetTableBuilder() {
  var soIn = List[String]()
  var taIn = List[String]()
  var scoIn = List[Double]()
  var dbIn = List[String]()

  def add(source: MiRNA, target: RefSeq, score: Double,
      db: String) {
    soIn ::= source.id
    taIn ::= target.id
    scoIn ::= score
    dbIn ::= db
  }

  def addAll(other: TargetTable) {
    for ((s, t, sc, db) <- other) {
      add(s, t, sc, db)
    }
  }

  def build =
    new TargetTable(soIn.toArray, taIn.toArray, scoIn.toArray, dbIn.toArray)
}
