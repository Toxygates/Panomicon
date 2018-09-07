/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import scala.collection.{ Map => CMap }

import t.db.SampleClassLike
import t.model.sample.Attribute
import t.model.shared.SampleClassHelper._

object SampleClassFilter {
  def apply(cl: t.model.SampleClass): SampleClassFilter =
    SampleClassFilter(cl.asScalaMap)
}

/**
 * A sample class with sparql methods.
 */
case class SampleClassFilter(constraints: CMap[Attribute, String] = Map()) extends SampleClassLike {

  def filterAll: Filter = {
    if (constraints.isEmpty) {
      Filter("", "")
    } else {
      val ptn = "?x " + constraints.keySet.map(k => s"t:${k.id} ?${k.id}").mkString(";") + "."
      val cnst = "FILTER(" + constraints.keySet.map(k => {
        "?" + k.id + " = \"" + constraints(k) + "\""
      }).mkString(" && ") + "). "
      Filter(ptn, cnst)
    }
  }

  def filter(key: Attribute): Filter = {
    if (!constraints.contains(key)) {
      Filter("", "")
    } else {

      Filter(s"?x t:${key.id} ?${key.id}.", "FILTER(?" + key.id + " = \"" + constraints(key) + "\").")
    }
  }
}
