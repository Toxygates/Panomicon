package t.platform.mirna

/**
 * Memory-efficient table for transcription factor targets.
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
