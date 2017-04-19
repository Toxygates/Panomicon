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

/**
 * A query that can be extended gradually and returns results of a particular
 * type.
 *
 * A model query is e.g.
 *
 * PREFIX x:<http://x.x.x/x>
 * PREFIX ...
 *
 * select * { ?x ?y ?z; ?a ?b ... ['pattern, extensible part']
 * } ... ['suffix']
 */

case class Query[+T](initPart: String, pattern: String,
  suffix: String = "\n}", eval: (String) => T) {
  def queryText: String = s"$initPart\n$pattern\n$suffix\n"

  def constrain(constraint: String): Query[T] = copy(pattern = pattern + "\n " + constraint)
  def constrain(filter: t.sparql.Filter): Query[T] =
    copy(pattern = pattern + filter.queryPattern + "\n " + filter.queryFilter)

  /**
   * Run the query
   */
  def apply(): T = eval(queryText)
}
