/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

trait RDFClass {
  def itemClass: String
  def defaultPrefix: String

  def packURI(name: String) = s"$defaultPrefix/$name"
  def unpackURI(uri: String) = uri.split(defaultPrefix + "/")(1)
}

object TRDF {
  //Reference: http://www.w3.org/TR/rdf-sparql-query/#grammarEscapes

  //For ID strings (may be part of URLs)
  val invalidChars = Set(' ', '\t', '\n', '\r', '\b', '\f', '"', '\'')

  val replacements = Map(
    '\t' -> "\\\t",
    '\n' -> "\\\n",
    '\r' -> "\\\r",
    '\b' -> "\\\b",
    '\"' -> "\\\"",
    '\'' -> "\\'",
    '\\' -> "\\\\")

  def isValidIdentifier(x: String) = !invalidChars.exists(c => x.contains(c))
  def checkValidIdentifier(x: String, typ: String) {
    if (!isValidIdentifier(x)) {
      throw new Exception(s"Invalid $typ: $x (spaces, quotation marks etc. are not allowed)")
    }
  }

  def escape(s: String): String = {
    val sb = new StringBuilder
    for (i <- 0 until s.length(); c = s.charAt(i)) {
      if (replacements.keySet.contains(c)) {
        sb.append(replacements(c))
      } else {
        sb.append(c)
      }
    }
    sb.toString
  }
}
