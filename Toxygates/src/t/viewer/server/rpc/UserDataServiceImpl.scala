package t.viewer.server.rpc

import t.viewer.client.rpc.UserDataService
import t.common.server.maintenance.BatchOpsImpl
import t.viewer.server.Configuration
import t.common.shared.maintenance.Batch
import t.common.shared.Dataset
import t.global.KCDBRegistry

/**
 * A servlet for managing user data (as batches).
 * In practice, this is a restricted variant of the maintenanc
 * servlet.
 */
abstract class UserDataServiceImpl extends TServiceServlet
  with BatchOpsImpl with UserDataService {
  private var homeDir: String = _

  override def localInit(config: Configuration) {
    super.localInit(config)
    homeDir = config.webappHomeDir
  }

  override protected def getAttribute[T](name: String) =
    getThreadLocalRequest().getSession().getAttribute(name).asInstanceOf[T]

  override protected def setAttribute(name: String, x: AnyRef): Unit =
     getThreadLocalRequest().getSession().setAttribute(name, x)

  override protected def request = getThreadLocalRequest

  override protected def updateBatch(b: Batch): Unit = {
    //Here, we must first ensure existence of the dataset.
    //For user data, the unique user id will here be supplied from the client side.

    val d = new Dataset(b.getDataset, "User data",
        "Automatically generated", null, "Automatically generated")
    addDataset(d, false)
    super.updateBatch(b)
  }

  override protected def afterTaskCleanup(): Unit = {
    super.afterTaskCleanup()
    KCDBRegistry.closeWriters()
  }
}
