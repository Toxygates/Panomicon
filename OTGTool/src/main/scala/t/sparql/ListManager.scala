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

import java.io.Closeable

import Triplestore.tPrefixes
import t.TriplestoreConfig
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat

/**
 * Manages a list of items of some given class in the triplestore.
 */
abstract class ListManager(config: TriplestoreConfig) extends Closeable {
  import Triplestore._

  //RDFS class
  def itemClass: String

  //URI prefix
  def defaultPrefix: String

  lazy val triplestore = config.get

  def close() {
    triplestore.close()
  }

  def list(): Seq[String] = {
    triplestore.simpleQuery(s"$tPrefixes\nSELECT ?l { ?x a $itemClass ; rdfs:label ?l }")
  }

  def verifyExists(item: String): Unit = {
    if (!list.contains(item)) {
      val msg = s"$item of class $itemClass does not exist"
      throw new Exception(msg)
    }
  }

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def addWithTimestamp(name: String, comment: String): Unit = {
    if (name.contains(' ')) {
      throw new Exception("Name must not contain spaces.")
    }
    if (comment.contains('"')) {
      throw new Exception("A comment may not contain the \" character.")
    }
    val encodedDate = dateFormat.format(new Date())

    triplestore.update(s"$tPrefixes\n INSERT DATA { <$defaultPrefix/$name> a $itemClass ; " +
      " rdfs:label \"" + name + "\"; t:comment \"" + comment + "\"; " +
      " t:timestamp \"" + encodedDate + "\". " +
      " }")
  }

  def add(name: String): Unit = {
    triplestore.update(s"$tPrefixes\n INSERT DATA { <$defaultPrefix/$name> a $itemClass ; " +
      " rdfs:label \"" + name + "\" }")
  }

  private def attributeQuery[T](attr: String, decode: String => T): Map[String, T] = {
    Map() ++ triplestore.mapQuery(s"$tPrefixes\nSELECT ?l ?att WHERE { ?item a $itemClass; rdfs:label ?l ; " +
      s"$attr ?att } ").map(x => {
      x("l") -> decode(x("att"))
    })
  }

  private val timestampRel = "t:timestamp"
  private val commentRel = "t:comment"
  private val publicCommentRel = "t:publicComment"

  def keyAttributes: Iterable[(String, Date, String, String)] = {
    val query = s"""|$tPrefixes
    |SELECT * WHERE {
    | ?item a $itemClass; $timestampRel ?timestamp;
    |   $commentRel ?comment; $publicCommentRel ?publicComment;
    |   rdfs:label ?label.
    |}
    |""".stripMargin

    val r = triplestore.mapQuery(query, 20000)
    r.map(x => {
      (x("label"), dateFormat.parse(x("timestamp")), x("comment"), x("publicComment"))
    })
  }

  def timestamps: Map[String, Date] =
    attributeQuery(timestampRel, dateFormat.parse)

  def comments: Map[String, String] =
    attributeQuery(commentRel, x => x)

  def publicComments: Map[String, String] =
    attributeQuery(publicCommentRel, x => x)

  def delete(name: String): Unit = {
    triplestore.update(s"$tPrefixes\n " +
      s"DELETE { <$defaultPrefix/$name> ?p ?o. } \n" +
      s"WHERE { <$defaultPrefix/$name> ?p ?o. } ")

    triplestore.update(s"$tPrefixes\n " +
      s"DELETE { ?s ?p <$defaultPrefix/$name> . } \n" +
      s"WHERE { ?s ?p <$defaultPrefix/$name> . } ")
  }

  def updateSinglePredicate(name: String, key: String, value: String): Unit = {
       triplestore.update(s"$tPrefixes\n " +
        s"DELETE { <$defaultPrefix/$name> $key ?o. } \n" +
        s"WHERE { <$defaultPrefix/$name> $key ?o. } ")
    triplestore.update(s"$tPrefixes\n " +
        s"INSERT DATA { <$defaultPrefix/$name> $key $value }")
  }

  def setComment(name: String, comment: String): Unit = {
    updateSinglePredicate(name, "t:comment", "\"" + comment + "\"")
  }

  def setPublicComment(name: String, comment: String): Unit = {
    updateSinglePredicate(name, "t:publicComment", "\"" + comment + "\"")
  }
}
