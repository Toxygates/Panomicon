package t.viewer.server.rpc

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest

import scala.collection.JavaConversions._
import scala.io._
import javax.servlet.ServletConfig
import t.viewer.server.Configuration

object GeneSetServlet { 
  val IMPORT_SESSION_KEY = "importedGenes"
}

class GeneSetServlet extends HttpServlet {
  import GeneSetServlet._
  
  import HttpServletResponse._
  
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = {
    println(req.getPathInfo)
    val request = Option(req.getPathInfo)
    request match {
      case Some(r) =>
        r.split("/").toSeq match {
          case "" +: "import" +: _ => doImport(req, resp)
          case _ => 
            Console.err.println(s"Unsupported request for GeneSetServlet: $request")
            resp.setStatus(SC_BAD_REQUEST)
        }
      case _ =>
        Console.err.println(s"Empty request for GeneSetServlet")
        resp.setStatus(SC_BAD_REQUEST)
    }
  }

  def getValidGene(g: String): Option[String] = {
    val GenePattern = """([A-Za-z0-9]+)""".r
    g match {
      case GenePattern(x) => Some(x)
      case _ => None
    }
  }
  
  def doImport(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val MAX_SIZE = 100000
    req.getPathInfo
    
    val url = req.getRequestURL
    
    //example: "http://127.0.0.1:8888/toxygates/#data"
    val REDIR_LOCATION = url.toString.split("geneSet/import")(0) + "#data"
    
    val is = req.getInputStream
    val s = Source.fromInputStream(is)
    val lines = s.getLines
    val allGenes =
      (lines.flatMap(_.split(",").map(
          word => getValidGene(word.trim))
          ) take MAX_SIZE).flatten.toArray

    if (lines.hasNext) {
      Console.err.println("Imported gene set is too large")
      resp.setStatus(SC_BAD_REQUEST)
      return
    }
    
    println(s"Importing ${allGenes.size} genes")
    req.getSession.setAttribute(IMPORT_SESSION_KEY, allGenes)
    
    resp.sendRedirect(REDIR_LOCATION)    
  }
  
}