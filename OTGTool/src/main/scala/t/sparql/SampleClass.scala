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

package t.sparql

import scala.collection.{Map => CMap}

object SampleClass {  
  def constraint(key: String, x: Option[String]) = x.map(key -> _)
}

case class SampleClass(constraints: CMap[String, String] = Map()) {
  
  def filterAll: Filter = {
    if (constraints.isEmpty) {
      Filter("", "")
    } else {
      val ptn = "?x " + constraints.keySet.map(k => s"t:$k ?$k").mkString(";") + "."
      val cnst = "FILTER(" + constraints.keySet.map(k => {
      "?" + k + " = \"" + constraints(k) + "\""
      }).mkString(" && ") + "). "
      Filter(ptn, cnst)
    }    
  }
  
  def filter(key: String): Filter = {
    if (!constraints.contains(key)) {
      Filter("", "")
    } else {
      Filter(s"?x t:$key ?$key.", "FILTER(?" + key + " = \"" + constraints(key) + "\").")
    }
  }
   
	def apply(key: String) = constraints(key)
	
	def get(key: String) = constraints.get(key)
}