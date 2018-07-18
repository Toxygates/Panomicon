package t.platform.mirna

import t.platform.Probe

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

  def asTriples: Iterable[(String, String, Double)] =
    (0 until size).map(i =>
      (sources(i), targets(i), scores(i)))

  def scoreFilter(minScore: Double): TargetTable = {
    val builder = new TargetTableBuilder
    for {
      i <- 0 until size;
      if scores(i) >= minScore
    } builder.add(sources(i), targets(i), scores(i))

    builder.build
  }
  
  /**
   * Find probes in the platform that match the given transcripts.
   * TODO: this could be a static lookup map?
   */
  def probesForTranscripts(platform: Iterable[Probe], transcripts: Iterable[String]): 
    Iterable[(String, Probe)] = {
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
  def targets(miRNAs: Iterable[String], platform: Iterable[Probe]): Iterable[(String, Probe)] = {
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
   * Efficient mRNA to miRNA lookup
   */
  def reverseTargets(mRNAs: Iterable[String], platform: Iterable[Probe]): Iterable[(Probe, String)] = {
    ???
  }
}

class TargetTableBuilder() {
  var soIn = List[String]()
  var taIn = List[String]()
  var scoIn = List[Double]()

  def add(source: String, target: String, score: Double) {
    soIn ::= source
    taIn ::= target
    scoIn ::= score
  }

  def build =
    new TargetTable(soIn.toArray, taIn.toArray, scoIn.toArray)
}
