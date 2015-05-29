/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.common.shared.probe

import t.common.shared.sample.ExpressionValue
import t.db.ExprValue

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
  def format(x: Double) = ExprValue.nf.format(x)
  
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
    
    
    val tooltip = "med(" + sorted.map(x => format(x.getValue)).mkString(", ") + ")"

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