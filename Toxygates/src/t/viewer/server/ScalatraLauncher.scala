package t.viewer.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

/**
 * Minimal application to run the Scalatra server (only) in Jetty.
 * It will identify ScalatraBootstrap and mount the ScalatraJSONServlet from there.
 */
object ScalatraLauncher {
  def main(args: Array[String]) {
    val port = if(System.getenv("PORT") != null) System.getenv("PORT").toInt else 8888

    val server = new Server(port)
    val context = new WebAppContext()
    context setContextPath "/"
    context.setResourceBase("/")

    /**
     * Minimal configuration for development of the scalatra server.
     * Based on essential settings from web.xml.
     * May be extended as needed.
     */
    val configuration = Map(
      "repositoryURL" -> "http://localhost:3030/Toxygates/query",
      "dataDir" -> "kcchunk:/shiba/scratch/toxygates/rebuild_test",
      "matrixDbOptions" -> "#pccap=1073741824#msiz=4294967296",
      "repositoryUser" -> "abc",
      "repositoryPassword" -> "xyz",
      "updateURL" -> "http://localhost:3030/Toxygates/update",
      "instanceName" -> "dev"
    )
    for { (k,v) <- configuration } {
      context.setInitParameter(k, v)
    }

    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start
    server.join
  }
}
