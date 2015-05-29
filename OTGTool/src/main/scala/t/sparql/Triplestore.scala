package t.sparql

import t.TriplestoreConfig
import org.openrdf.repository.RepositoryConnection
import org.openrdf.query.QueryLanguage
import scala.concurrent.Await
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeoutException
import java.util.concurrent.Executors
import org.openrdf.repository.sparql.SPARQLRepository
import java.net.ProxySelector
import org.openrdf.repository.manager.RemoteRepositoryManager
import t.Closeable
import org.openrdf.rio.RDFFormat
import org.openrdf.model.Resource
import org.openrdf.model.impl.URIImpl

object Triplestore {  
  val executor = Executors.newCachedThreadPool()
  val executionContext = ExecutionContext.fromExecutor(executor)
  
  val tPrefixes: String = """
    PREFIX purl:<http://purl.org/dc/elements/1.1/>
    PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX owl:<http://www.w3.org/2002/07/owl#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> 
    PREFIX t:<http://level-five.jp/t/>"""

  /*
   * TODO: Currently we use connectRemoteRepository for Owlim-SE connections, and
   * SPARQLRepository for all other connections, but there should be no 
   * need to have two connection methods. 
   * Aim to use only SPARQLRepository in the future. 
   */
    
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
    c.setAutoCommit(true)
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
  
  def con: RepositoryConnection 
    
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
    pq.setMaxQueryTime(timeoutMillis / 1000)
    val f = future { pq.evaluate() }
    Some(Await.result(f, Duration(timeoutMillis, "millis")))    
  }
  
  /**
   * Perform a SPARQL update.
   */
  def update(query: String, quiet: Boolean = false): Unit = {
    if (!quiet) {
      println(query)
    }
    try {
      val pq = con.prepareUpdate(QueryLanguage.SPARQL, query)
      pq.execute()
    } catch {
      case e: Exception =>
        Console.err.println("Exception on query: " + query)
        throw e
    }
  }
  
  /**
   * Insert a TRIG file.
   */
  def addTTL(file: java.io.File, context: String): Unit = {
    println(s"Insert file $file into $context")
    con.add(file, null, RDFFormat.TURTLE, new URIImpl(context))    
  }
  
  def simpleQueryNonQuiet(query: String): Vector[String] = simpleQuery(query, true)
  
  /**
   * Query for some number of records, each containing a single field.
   */
  def simpleQuery(query: String, quiet: Boolean = false)(implicit timeoutMillis: Int = 10000): Vector[String] = {
    var r: Vector[String] = Vector.empty
    evaluate(query, timeoutMillis).foreach(rs => {
      while (rs.hasNext) {
        val tuple = rs.next
        val it = tuple.iterator
        while (it.hasNext) {
          val v = it.next.getValue
          r +:= v.stringValue
        }
      }
      rs.close
    })
    if (!quiet) {
    	println(if (r.size > 10) { "[" + r.size + "] " + r.take(5) + " ... " } else { r })
    }
    r
  }

  /**
   * Query for some number of records, each containing some number of fields.
   */
  def multiQuery(query: String)(implicit timeoutMillis: Int = 10000): Vector[Vector[String]] = {
    val start = System.currentTimeMillis()
    var r: Vector[Vector[String]] = Vector.empty
    evaluate(query, timeoutMillis).foreach(rs => {
      while (rs.hasNext) {
        val tuple = rs.next
        val it = tuple.iterator
        var current: Vector[String] = Vector.empty
        while (it.hasNext) {
          val v = it.next.getValue
          current +:= v.stringValue
        }
        r +:= current
      }
      rs.close
    })
    println("Took " + (System.currentTimeMillis() - start) / 1000.0 + " s")

    println(if (r.size > 10) { r.take(10) + " ... " } else { r })
    r
  }

  /**
   * Query for some number of records, each containing named fields.
   * TODO this could be optimised, no need to construct the map each time if
   * some value handler is passed in
   */
  def mapQuery(query: String)(implicit timeoutMillis: Int = 10000): Vector[Map[String, String]] = {
    var r: Vector[Map[String, String]] = Vector.empty
    evaluate(query, timeoutMillis).foreach(rs => {
      while (rs.hasNext) {
        val tuple = rs.next
        val it = tuple.iterator
        var current: Map[String, String] = Map()
        while (it.hasNext) {
          val n = it.next
          current += (n.getName -> n.getValue.stringValue)
        }
        r +:= current
      }

      rs.close
    })
    println(if (r.size > 10) { r.take(10) + " ... " } else { r })
    r
  }
}

class SimpleTriplestore(val con: RepositoryConnection) extends Triplestore
