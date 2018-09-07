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

import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

import org.eclipse.rdf4j.common.iteration.Iteration
import org.eclipse.rdf4j.model.impl.URIImpl
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.rio.RDFFormat

import t.Closeable
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

object Triplestore {
  val executor = Executors.newCachedThreadPool()
  val executionContext = ExecutionContext.fromExecutor(executor)

  val tPrefixes: String = """PREFIX purl:<http://purl.org/dc/elements/1.1/>
    |PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    |PREFIX owl:<http://www.w3.org/2002/07/owl#>
    |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    |PREFIX t:<http://level-five.jp/t/>""".stripMargin.replace('\n', ' ')

  def connectSPARQLRepository(queryUrl: String, updateUrl: String = null,
    user: String = null, pass: String = null): RepositoryConnection = {
    println("Initialize SPARQL connection for " + queryUrl)
    val rep = if (updateUrl != null && updateUrl != "") {
      new SPARQLRepository(queryUrl, updateUrl)
    } else {
      new SPARQLRepository(queryUrl)
    }
    if (user != null && pass != null) {
      rep.setUsernameAndPassword(user, pass)
    }
    rep.initialize()
    if (rep == null) {
      throw new Exception("Unable to access repository ")
    }

    println("Get connection")
    rep.getConnection
  }
}

abstract class Triplestore extends Closeable {

  val PRINT_QUERIES = true
  val PRINT_RESULTS = true

  def isReadonly: Boolean = true

  protected def con: RepositoryConnection

  def close() {
    con.close()
  }

  // Necessary for futures
  private[this] implicit val executionContext = Triplestore.executionContext

  /**
   * Perform a SPARQL query.
   */
  @throws(classOf[TimeoutException])
  private def evaluate(query: String, timeoutMillis: Int = 10000) = {
  /* Note: it might be better to return a future rather than wait for the future
   * to complete here, so that queries become composable
   * (although at the moment, almost all RPC calls we do
   * need one query result only and they need to wait for it)
   */

    if (PRINT_QUERIES) println
    printHash("printing query:", query)
    if (PRINT_QUERIES) println(query)
    val pq = con.prepareTupleQuery(QueryLanguage.SPARQL, query)
    pq.setMaxExecutionTime(timeoutMillis / 1000)
    pq.evaluate()
  }

  /**
   * Perform a SPARQL update.
   */
  def update(query: String, quiet: Boolean = false): Unit = {
    if (!quiet) {
      if (PRINT_QUERIES) println('\n' + query)
    }
    if (isReadonly) {
      println("Triplestore is read-only, ignoring update query")
    } else {
      try {
        val pq = con.prepareUpdate(QueryLanguage.SPARQL, query)
        pq.setMaxExecutionTime(0)
        pq.execute()
      } catch {
        case e: Exception =>
          Console.err.println("Exception on query: " + query)
          throw e
      }
    }
  }

  /**
   * Insert a TRIG file.
   */
  def addTTL(file: java.io.File, context: String): Unit = {
    if (isReadonly) {
      println(s"Triplestore is read-only, ignoring data insertion of $file into $context")
    } else {
      println(s"Insert file $file into $context")
      con.add(file, null, RDFFormat.TURTLE, SimpleValueFactory.getInstance.createIRI(context))
    }
  }

  def simpleQueryNonQuiet(query: String): Vector[String] = simpleQuery(query, false)

  import scala.language.implicitConversions
  private implicit def resultToSeq[T, U <: Exception](i: Iteration[T,U]) = {
    var r: Vector[T] = Vector()
    while (i.hasNext) {
      r :+= i.next
    }
    r
  }

  /**
   * Query for some number of records, each containing a single field.
   */
  def simpleQuery(query: String, quiet: Boolean = false, timeoutMillis: Int = 10000): Vector[String] = {
    val start = System.currentTimeMillis()
    val rs = evaluate(query, timeoutMillis)
    val recs = for (
      tuple <- rs;
      v <- tuple.asScala;
      s = v.getValue.stringValue()
    ) yield s
    rs.close
    if (!quiet) {
      logQueryStats(recs, start, query)
    }
    recs
  }

  /**
   * Query for some number of records, each containing some number of fields.
   */
  def multiQuery(query: String, timeoutMillis: Int = 10000): Vector[Vector[String]] = {
    val start = System.currentTimeMillis()
    val rs = evaluate(query, timeoutMillis)
    val recs = for (
      tuple <- rs;
      rec = tuple.asScala.map(_.getValue.stringValue)
    ) yield rec.toVector
    rs.close
    logQueryStats(recs, start, query)
    recs
  }

  /**
   * Query for some number of records, each containing named fields.
   */
  def mapQuery(query: String, timeoutMillis: Int = 10000): Vector[Map[String, String]] = {
    val start = System.currentTimeMillis()
    val rs = evaluate(query, timeoutMillis)
    val recs = for (
      tuple <- rs;
      rec = Map() ++ tuple.asScala.map(n => n.getName -> n.getValue.stringValue())
    ) yield rec
    rs.close
    logQueryStats(recs, start, query)
    recs
  }

  def logQueryStats(recs: Vector[Object], start: Long, query: String) {
    printHash("printing query result:", query)
    if (PRINT_RESULTS) {
      println("Found " + recs.size + " results in " + (System.currentTimeMillis() - start) / 1000.0 + "s:")
      println(if (recs.size > 10) { recs.take(10) + " ... " } else { recs })
    }
  }

  val DEBUG_LOG_HASHES = false

  def printHash(postfix: String, obj: Object) = {
    if (DEBUG_LOG_HASHES) {
      val hash = obj.hashCode
      println(s"Hashcode $hash $postfix")
    }
  }
}

class SimpleTriplestore(val con: RepositoryConnection, override val isReadonly: Boolean) extends Triplestore {
  if (isReadonly) {
    println("SPARQL READ ONLY MODE - no RDF data will be inserted or updated")
  }
}
