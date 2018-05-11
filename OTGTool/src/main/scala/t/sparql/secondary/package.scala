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

/**
 * This package contains SPARQL APIs for annotations to
 * probes or samples, etc. The data may be stored locally or remotely.
 */
package object secondary extends QueryUtils {
  import Triplestore._

  //TODO: local is due for retirement
   val commonPrefixes = s"""$tPrefixes
    |PREFIX local:<http://127.0.0.1:3333/>
    |PREFIX bio2rdf:<http://bio2rdf.org/ns/bio2rdf#>""".stripMargin.replace('\n', ' ')

  def unpackGeneid(geneid: String) = geneid.split("geneid:")(1)
}
