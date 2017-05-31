/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

trait QueryUtils {
  /**
   * Filtering the variable v to be in a given list of values.
   * Note: use with caution! FILTER( IN (...)) often leads to slow queries.
   * Consider using the unions below instead.
   */
  def multiFilter(v: String, values: Iterable[String]): String = {
    if (values.isEmpty) {
      ""
    } else {
      " FILTER(" + v + " IN(\n" + values.grouped(10).map("\t" + _.mkString(",")).
        mkString(",\n") + "\n)) \n"
    }
  }

  def multiFilter(v: String, values: Option[Iterable[String]]): String =
    multiFilter(v, values.getOrElse(Iterable.empty))

  def multiUnion(subjObjs: Iterable[(String, String)], pred: String): String =
    subjObjs.grouped(3).map(xs => xs.map(x => "{ " + x._1 + " " + pred + " " + x._2 + " . } ").
      mkString(" UNION ")).mkString(" \n UNION \n ")

  def multiUnionSubj(subjects: Iterable[String], pred: String, obj: String): String =
    multiUnion(subjects.map(s => (s, obj)), pred)

  def multiUnionObj(subject: String, pred: String, objects: Iterable[String]): String =
    multiUnion(objects.map(o => (subject, o)), pred)

  def caseInsensitiveMultiFilter(v: String, values: Iterable[String]): String = {
    val m = values.map(" regex(" + v + ", " + _ + ", \"i\") ")
    if (m.size == 0) {
      ""
    } else {
      "FILTER ( " + m.mkString(" || ") + " )"
    }
  }
}

object QueryUtils extends QueryUtils
