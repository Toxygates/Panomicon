package t.db

/**
 * A series of expression values ranging over some independent variable
 */
abstract class Series[This <: Series[This]](val probe: Int, val points: Seq[SeriesPoint]) {
  this: This =>
  def classCode(implicit mc: MatrixContext): Long  
  
  def addPoints(from: This, builder: SeriesBuilder[This]): This = {
    val keep = points.filter(x => ! from.points.exists(y => y.code == x.code))
    builder.rebuild(this, keep ++ from.points)
  }

  def removePoints(in: This, builder: SeriesBuilder[This]): This = {
    val keep = points.filter(x => !in.points.exists(y => y.code == x.code))
    builder.rebuild(this, keep)
  }
  
  def values = points.map(_.value)
  
  def presentValues = points.map(_.value).filter(_.call != 'A').map(_.value)
    
  def probeStr(implicit mc: MatrixContext) = mc.probeMap.unpack(probe)
  
  /**
   * A modified (less constrained) version of this series that 
   * represents the constraints for ranking with a single probe.
   */
  def asSingleProbeKey: This
}

/**
 * Code is the encoded enum value of the independent variable, e.g.
 * 24hr for a time series.
 */
case class SeriesPoint(code: Int, value: ExprValue)

/**
 * An object that can decode/encode the database format of series 
 * for a particular application.
 */
trait SeriesBuilder[S <: Series[S]] {
  /**
   * Construct a series with no points.
   */
  def build(sampleClass: Long, probe: Int)(implicit mc: MatrixContext): S
  
  /**
   * Insert points into a series.
   */
  def rebuild(from: S, points: Iterable[SeriesPoint]): S
  
  /**
   * Generate all keys belonging to the (partially specified)
   * series key. 
   */
  def keysFor(group: S)(implicit mc: MatrixContext): Iterable[S]
  
  /**
   * Using values from the given MatrixDB, construct all possible series for the
   * samples indicated in the metadata.
   */
  def makeNew[E >: Null <: ExprValue](from: MatrixDBReader[E], md: Metadata, samples: Iterable[Sample])
  	(implicit mc: MatrixContext): Iterable[S]
  
  /**
   * Using values from the given MatrixDB, construct all possible series for the
   * samples indicated in the metadata.
   */
  def makeNew[E >: Null <: ExprValue](from: MatrixDBReader[E], md: Metadata)
  	(implicit mc: MatrixContext): Iterable[S]
		  = makeNew(from, md, md.samples)

  /**
   * Enum keys that are necessary for this SeriesBuilder.
   */
  def enums: Iterable[String]
  
  def standardEnumValues: Iterable[(String, String)]

  /**
   * Expected time points for the given series
   */
  def expectedTimes(key: S): Seq[String]
  
  protected def packWithLimit(enum: String, field: String, mask: Int)(implicit mc: MatrixContext) = {
    val p = mc.enumMaps(enum)(field)
    if (p > mask) {
      throw new Exception(s"Unable to pack Series: $field in '$enum' is too big ($p)")
    }
    (p & mask).toLong
  }

  def presentMean(ds: Iterable[Iterable[ExprValue]]): Iterable[ExprValue] = {
    var byProbe = Map[String, List[ExprValue]]()
    for (xs <- ds; y <- xs; p = y.probe) {
      if (byProbe.contains(p)) {
        byProbe += (p -> (y :: byProbe(p)))
      } else {
        byProbe += (p -> List(y))
      }
    }
    byProbe.map(x => ExprValue.presentMean(x._2, x._1))
  } 
}
