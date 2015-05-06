package t.sparql

import Triplestore.tPrefixes
import t.TriplestoreConfig
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat
import t.Closeable

/**
 * Manages a list of items of some given class in the triplestore.
 */
abstract class ListManager(config: TriplestoreConfig) extends Closeable {
  import Triplestore._

  //RDFS class
  def itemClass: String
  
  //URI prefix
  def defaultPrefix: String

  lazy val ts = new SimpleTriplestore(config.triplestore)

  def close() {
    ts.close()
  }
  
  def list(): Seq[String] = {
    ts.simpleQuery(s"$tPrefixes\n select ?l { ?x a $itemClass ; rdfs:label ?l }")
  }
  
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")  

  def addWithTimestamp(name: String, comment: String): Unit = {
    if (comment.contains('"')) {
      throw new Exception("A comment may not contain the \" character.")
    }    
    val encodedDate = dateFormat.format(new Date())
    
    ts.update(s"$tPrefixes\n insert data { <$defaultPrefix/$name> a $itemClass ; " +
      " rdfs:label \"" + name + "\"; t:comment \"" + comment + "\"; " +
      " t:timestamp \"" + encodedDate + "\". " +
      " }")
  }
  
  def add(name: String): Unit = {
      ts.update(s"$tPrefixes\n insert data { <$defaultPrefix/$name> a $itemClass ; " +
      " rdfs:label \"" + name + "\" }")
  }
  
  def timestamps: Map[String, Date] = {
    Map() ++ ts.mapQuery(s"$tPrefixes select ?l ?time where { ?item a $itemClass; rdfs:label ?l ; " +
        "t:timestamp ?time } ").map(x => {
          x("l") -> dateFormat.parse(x("time"))
        })
  }
  
  def comments: Map[String, String] = {
    Map() ++ ts.mapQuery(s"$tPrefixes select ?l ?com where { ?item a $itemClass; rdfs:label ?l ; " +
        "t:comment ?com } ").map(x => {
          x("l") -> x("com")
        })
  }

  def delete(name: String): Unit = {
    ts.update(s"$tPrefixes\n " +
      s"delete { <$defaultPrefix/$name> ?p ?o. } \n" +
      s"where { <$defaultPrefix/$name> ?p ?o. } ")

    ts.update(s"$tPrefixes\n " +
      s"delete { ?s ?p <$defaultPrefix/$name> . } \n" +
      s"where { ?s ?p <$defaultPrefix/$name> . } ")         
  }
}