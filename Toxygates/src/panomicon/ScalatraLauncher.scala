package panomicon

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
    val port = if (System.getenv("PORT") != null) System.getenv("PORT").toInt else 8888

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
      "repositoryURL" -> System.getenv("T_TS_URL"),
      "dataDir" -> System.getenv("T_DATA_DIR"),
      "matrixDbOptions" -> System.getenv("T_DATA_MATDBCONFIG"),
      "repositoryUser" -> System.getenv("T_TS_USER"),
      "repositoryPassword" -> System.getenv("T_TS_PASS"),
      "updateURL" -> System.getenv("T_TS_UPDATE_URL"),
      "instanceName" -> System.getenv("T_INSTANCE_NAME")
    )
    for {(k, v) <- configuration} {
      context.setInitParameter(k, v)
    }

    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start
    server.join
  }
}
