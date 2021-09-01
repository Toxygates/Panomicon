import javax.servlet.ServletContext
import t.server.viewer.servlet._
import org.scalatra._
import panomicon.ScalatraJSONServlet

/**
 * Configuration file for Scalatra. Must be in the root package to be
 * discovered.
 */
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new ScalatraJSONServlet(context), "/json/*")
  }
}
