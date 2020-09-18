import javax.servlet.ServletContext
import t.viewer.server.servlet._
import org.scalatra._

/**
 * Configuration file for Scalatra. Must be in the root package to be
 * discovered.
 */
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new ScalatraJSONServlet(context), "/*")
  }
}
