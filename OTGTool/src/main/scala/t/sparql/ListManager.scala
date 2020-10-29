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
import java.text.SimpleDateFormat
import java.util.Date

import t.TriplestoreConfig


/**
 * Attributes that we expect each managed item to have in the majority of cases.
 * @param id
 * @param timestamp
 * @param comment
 * @param publicComment
 */
case class KeyItemAttributes(id: String, timestamp: Date,
                             comment: String, publicComment: String)

/**
 * Manages a list of items of some given class in the triplestore.
 */
abstract class ListManager[T](config: TriplestoreConfig) extends Closeable {
  import Triplestore._

  //RDFS class
  def itemClass: String

  //URI prefix
  def defaultPrefix: String

  lazy val triplestore = config.getTriplestore()

  def close() {
    triplestore.close()
  }

  /**
   * Obtain the IDs of items in this list manager.
   * @return
   */
  def getList(): Seq[String] = getList("")

  def getList(additionalFilter: String): Seq[String] = {
    triplestore.simpleQuery(s"""|
                            |$tPrefixes
                            | SELECT ?l {
                            |  ?item a $itemClass; rdfs:label ?l.
                            |  $additionalFilter
                            | }""".stripMargin)
  }

  def verifyExists(item: String): Unit = {
    if (!getList().contains(item)) {
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

  /**
   * Obtain core attributes that managed items can reasonably be expected to have.
   */
  def getKeyAttributes(additionalFilter: String = ""): Iterable[KeyItemAttributes] = {
    val query = s"""|$tPrefixes
    |SELECT * WHERE {
    | ?item a $itemClass; rdfs:label ?label.
    | OPTIONAL { ?item $timestampRel ?timestamp. }
    | OPTIONAL { ?item $commentRel ?comment. }
    | OPTIONAL { ?item $publicCommentRel ?publicComment. }
    | $additionalFilter
    |}
    |""".stripMargin

    val defaultTimestamp = "1970-01-01 00:00:00"

    val r = triplestore.mapQuery(query, 20000)
    r.map(x => {
      KeyItemAttributes(x.getOrElse("label", ""), dateFormat.parse(x.getOrElse("timestamp", defaultTimestamp)),
        x.getOrElse("comment", ""), x.getOrElse("publicComment", ""))
    })
  }

  /**
   * Efficiently obtain the items in this list manager, with as many attributes
   * as possible populated (possibly not all).
   * @param instanceUri The instance to filter items for, if any
   * @return
   */
  def getItems(instanceUri: Option[String] = None): Iterable[T] = {
    ???
  }

  def getTimestamps(): Map[String, Date] =
    attributeQuery(timestampRel, dateFormat.parse)

  def getComments(): Map[String, String] =
    attributeQuery(commentRel, x => x)

  def getPublicComments(): Map[String, String] =
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
