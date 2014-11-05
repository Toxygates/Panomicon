package t.common.shared.probe

import t.common.shared.sample.ExpressionValue

/**
 * A value mapper combines all values for a given probe in the range
 * into a single domain value.
 * Example: combine multiple gene values into a single protein value.
 * Example: combine multiple transcript values into a single gene value.
 */

trait ValueMapper {

  /**
   * @return the domain value.
   */
  def convert(rangeProbe: String, domainVs: Iterable[ExpressionValue]): ExpressionValue
  
}

object MedianValueMapper extends ValueMapper {
  def convert(rangeProbe: String, domainVs: Iterable[ExpressionValue]): ExpressionValue = {
    if (domainVs.size == 0) {
      return new ExpressionValue(0.0, 'A', "(absent)")
    }
    
    //TODO call handling here
    val sorted = domainVs.toList.sortWith(_.getValue < _.getValue)
    val mid = domainVs.size / 2
    val nv = if (domainVs.size % 2 == 0) {
      (sorted(mid - 1).getValue + sorted(mid).getValue) / 2
    } else {
      sorted(mid).getValue
    }
    
    val tooltip = "med(" + sorted.mkString(", ") + ")"

    var call = 0d
    for (v <- domainVs) {
      v.getCall() match {
        case 'M' => call += 1.0
        case 'P' => call += 2.0
        case _ => {}
      }
    }
    val nc = Math.round(call/domainVs.size)
    val rc = if (nc == 2) 'P' else (if (nc == 1) 'M' else 'A')
    new ExpressionValue(nv, rc, tooltip)    
  }
}