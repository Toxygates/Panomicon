package t.platform.mirna

import t.sparql._
import t.platform._
import t.db._
import t.platform.Species.Species

/**
 * Memory-efficient table for transcription factor targets.
 *
 * Convention: sources are miRNAs such as hsa-let-7a-2-3p,
 * targets are mRNAs (identified by refSeq transcripts
 * such as NM_133594)
 */
class TargetTable(val sources: Array[String],
    val targets: Array[String],
    val scores: Array[Double]) {

  def size: Int = sources.length

  def asTriples: Iterable[(MiRNA, RefSeq, Double)] =
    (0 until size).map(i =>
      (MiRNA(sources(i)), RefSeq(targets(i)), scores(i)))

  def filterWith(test: Int => Boolean): TargetTable = {
    val builder = new TargetTableBuilder
    for {
      i <- 0 until size;
      if test(i)
    } builder.add(MiRNA(sources(i)), RefSeq(targets(i)), scores(i))

    builder.build
  }

  def scoreFilter(minScore: Double): TargetTable =
    filterWith(scores(_) >= minScore)

  def speciesFilter(species: Species): TargetTable = {
    val shortCode = species.shortCode
    filterWith(sources(_).startsWith(s"${shortCode}-"))
  }

  /**
   * Find probes in the platform that match the given transcripts.
   * TODO: this could be a static lookup map?
   */
  def probesForTranscripts(platform: Iterable[Probe], transcripts: Iterable[RefSeq]):
    Iterable[(RefSeq, Iterable[Probe])] = {
    val allTrn = transcripts.toSet
    val r = for {
      p <- platform.toSeq;
      foundTrn <- p.transcripts.filter(allTrn.contains)
    } yield (foundTrn, p)
    r.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
   * Efficient miRNA to mRNA lookup.
   * Probes in the platform must have transcripts populated.
   * TODO this is not fast enough yet.
   */
  def targets(miRNAs: Iterable[MiRNA], platform: Iterable[Probe]): Iterable[(MiRNA, Probe, Double)] = {
    val allMicro = miRNAs.toSet
    val allTrn = for {
      (source, target, score) <- asTriples;
      if (allMicro.contains(source))
    } yield (source, target, score)
    val probeLookup = Map() ++ probesForTranscripts(platform, allTrn.map(_._2))
    allTrn.flatMap(x => probeLookup.get(x._2) match {
      case Some(ps) => ps.map((x._1, _, x._3))
      case _ => Seq()
    })
  }

  /**
   * Efficient mRNA to miRNA lookup.
   * Probes must have transcripts populated.
   */
  def reverseTargets(mRNAs: Iterable[Probe]): Iterable[(Probe, MiRNA, Double)] = {
    val allTrns = mRNAs.flatMap(p => p.transcripts.map(tr => (tr, p)))
    val allTrLookup = allTrns.groupBy(_._1)
    val allTrKeys = allTrLookup.keySet
    println(s"Size ${allTrKeys.size} transcript key set")
    for {
        (source, target, score) <- asTriples;
        if allTrKeys contains target;
        (refSeq, probe) <- allTrLookup(target) }
    yield (probe, source, score)
  }

  final def limitSize[T](data: Iterable[T], limit: Option[Int]) = limit match {
    case Some(n) => data take n
    case None => data
  }

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
            (x._1.asProbe, DefaultBio(x._2.identifier, x._2.identifier, Some("miRDB 5.0")))
          ))
    } else {
      makeMultiMap(limitSize(reverseTargets(probes), sizeLimit).map(x =>
        (x._1, DefaultBio(x._2.id, x._2.id, Some("miRDB 5.0")))))
    }
  }
}

class TargetTableBuilder() {
  var soIn = List[String]()
  var taIn = List[String]()
  var scoIn = List[Double]()

  def add(source: MiRNA, target: RefSeq, score: Double) {
    soIn ::= source.id
    taIn ::= target.id
    scoIn ::= score
  }

  def build =
    new TargetTable(soIn.toArray, taIn.toArray, scoIn.toArray)
}
