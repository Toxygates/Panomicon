package t.platform.mirna

import t.sparql._
import t.platform._
import t.db._

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

  def scoreFilter(minScore: Double): TargetTable = {
    val builder = new TargetTableBuilder
    for {
      i <- 0 until size;
      if scores(i) >= minScore
    } builder.add(MiRNA(sources(i)), RefSeq(targets(i)), scores(i))

    builder.build
  }
  
  /**
   * Find probes in the platform that match the given transcripts.
   * TODO: this could be a static lookup map?
   */
  def probesForTranscripts(platform: Iterable[Probe], transcripts: Iterable[RefSeq]): 
    Iterable[(RefSeq, Probe)] = {
    val allTrn = transcripts.toSet
    for {
      p <- platform;
      foundTrn <- p.transcripts.toSet.intersect(allTrn)
    } yield (foundTrn, p)      
  }
  
  /**
   * Efficient miRNA to mRNA lookup.
   * Probes in the platform must have transcripts populated.
   */
  def targets(miRNAs: Iterable[MiRNA], platform: Iterable[Probe]): Iterable[(MiRNA, Probe)] = {
    val allMicro = miRNAs.toSet
    val allTrn = for {
      (source, target, score) <- asTriples;
      if (allMicro.contains(source))
    } yield (source, target)
    val probeLookup = Map() ++ probesForTranscripts(platform, allTrn.map(_._2))
    allTrn.flatMap(x => probeLookup.get(x._2) match {
      case Some(p) => Some((x._1, p))
      case _ => None
    })
  }
  
  /**
   * Efficient mRNA to miRNA lookup.
   * Probes in the platform must have transcripts populated.
   */
  def reverseTargets(mRNAs: Iterable[Probe], platform: Iterable[Probe]): Iterable[(Probe, MiRNA)] = {
    val allTrns = mRNAs.flatMap(p => p.transcripts.map(tr => (tr, p)))
    val allTrLookup = allTrns.groupBy(_._1) 
    val allTrKeys = allTrLookup.keySet
    println(s"Size ${allTrKeys.size} transcript key set")
    for {
        (source, target, score) <- asTriples;
        if allTrKeys contains target;
        (refSeq, probe) <- allTrLookup(target) }
    yield (probe, source)                   
  }
  
  /**
   * Convenience method
   */
  def associationLookup(probes: Iterable[Probe],
                     fromMirna: Boolean,
                     platform: Iterable[Probe]): MMap[Probe, DefaultBio] = {
    if (fromMirna) {
      ???
    } else {
      makeMultiMap(reverseTargets(probes, platform).map(x => 
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
