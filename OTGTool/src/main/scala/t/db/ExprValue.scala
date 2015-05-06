package t.db
import java.text.NumberFormat
import t.platform.Probe
import t.platform.SimpleProbe

/**
 * TODO: ExprValue should move to some subpackage of t
 */
object ExprValue {
  def presentMean(vs: Iterable[ExprValue], probe: String): ExprValue = {    
    val nps = vs.filter(_.call != 'A')
    if (nps.size > 0) {
      apply(nps.map(_.value).sum / nps.size, 'P', probe)
    } else {
      apply(0, 'A', probe)
    }
  }
  
  def apply(v: Double, call: Char = 'P', probe: String = "") = BasicExprValue(v, call, Probe(probe))
  
  val nf = NumberFormat.getNumberInstance()
}

trait ExprValue {
  def value: Double
  def call: Char
  def probe: Probe
  
  // Currently we interpret both P and M as present
  def present: Boolean = (call != 'A')
  
  override def toString(): String = 
  	s"(${ExprValue.nf.format(value)}:$call)" 
}

/**
 * An expression value for one probe in a microarray sample, with an associated
 * call. Call can be P, A or M (present, absent, marginal).
 */
case class BasicExprValue(value: Double, call: Char = 'P', probe: Probe = null) extends ExprValue 

/**
 * An expression value that also has an associated p-value.
 * The p-value is associated with fold changes in the smallest 
 * sample group that the sample belongs to (all samples with identical
 * experimental conditions).
 */
case class PExprValue(value: Double, p: Double, call: Char = 'P', probe: Probe = null) extends ExprValue

