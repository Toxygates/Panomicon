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

import t.TriplestoreConfig
import scala.concurrent.Await
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._
import java.util.concurrent.TimeoutException
import java.util.concurrent.Executors
import java.net.ProxySelector
import t.Closeable
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.common.iteration.Iteration
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.model.impl.URIImpl
import org.apache.http.params.HttpConnectionParams

object Triplestore {
  val executor = Executors.newCachedThreadPool()
  val executionContext = ExecutionContext.fromExecutor(executor)

  val tPrefixes: String = "\n" + """PREFIX purl:<http://purl.org/dc/elements/1.1/>
    |PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    |PREFIX owl:<http://www.w3.org/2002/07/owl#>
    |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    |PREFIX t:<http://level-five.jp/t/>""".stripMargin.replace('\n', ' ')

  /*
   * TODO: Currently we use connectRemoteRepository for Owlim-SE connections, and
   * SPARQLRepository for all other connections, but there should be no
   * need to have two connection methods.
   * Aim to use only SPARQLRepository in the future.
   */

  @deprecated("Being replaced with SPARQL", "Jan 2017")
  def connectRemoteRepository(config: TriplestoreConfig): RepositoryConnection = {
    println("Initialize remote repository connection for " + config.url)
    val repMan = RemoteRepositoryManager.getInstance(config.url, config.user, config.pass)
    repMan.initialize()

    val rep = if (config.repository != null && config.repository != "") {
      repMan.getRepository(config.repository)
    } else {
      repMan.getSystemRepository()
    }

    if (rep == null) {
      throw new Exception("Unable to select repository " + config.repository)
    }
    val c = rep.getConnection
    c
  }

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

  def isReadonly: Boolean = true

  protected def con: RepositoryConnection

  def close() {
    con.close()
  }

  // Necessary for futures
  private[this] implicit val executionContext = Triplestore.executionContext

  /**
   * Perform a SPARQL query.
   *
   * TODO: might be better to return a future rather than wait for the future
   * to complete here, so that queries become composable
   * (although at the moment, almost all RPC calls we do
   * need one query result only and they need to wait for it)
   */
  @throws(classOf[TimeoutException])
  private def evaluate(query: String, timeoutMillis: Int = 10000) = {
    println(query)
    val pq = con.prepareTupleQuery(QueryLanguage.SPARQL, query)
    // for sesame 2.7
//    pq.setMaxQueryTime(timeoutMillis / 1000)
    // for sesame 2.8

    pq.setMaxExecutionTime(timeoutMillis / 1000)
    pq.evaluate()
  }

  /**
   * Perform a SPARQL update.
   */
  def update(query: String, quiet: Boolean = false): Unit = {
    if (!quiet) {
      println(query)
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
      con.add(file, null, RDFFormat.TURTLE, new URIImpl(context))
    }
  }

  def simpleQueryNonQuiet(query: String): Vector[String] = simpleQuery(query, true)

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
      v <- tuple;
      s = v.getValue.stringValue()
    ) yield s
    rs.close
    if (!quiet) {
      logQueryStats(recs, start)
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
      rec = tuple.map(n => n.getValue.stringValue)
    ) yield rec.toVector
    rs.close
    logQueryStats(recs, start)
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
      rec = Map() ++ tuple.map(n => n.getName -> n.getValue.stringValue())
    ) yield rec
    rs.close
    logQueryStats(recs, start)
    recs
  }

  def logQueryStats(recs: Vector[Object], start: Long) {
    println("Found " + recs.size + " results in " + (System.currentTimeMillis() - start) / 1000.0 + "s:")
    println(if (recs.size > 10) { recs.take(10) + " ... " } else { recs })
  }
}

class SimpleTriplestore(val con: RepositoryConnection, override val isReadonly: Boolean) extends Triplestore {
  if (isReadonly) {
    println("SPARQL READ ONLY MODE - no RDF data will be inserted or updated")
  }
}
