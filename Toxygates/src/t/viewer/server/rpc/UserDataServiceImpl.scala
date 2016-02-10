package t.viewer.server.rpc

import t.viewer.client.rpc.UserDataService
import t.common.server.maintenance.BatchOpsImpl
import t.viewer.server.Configuration

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
}
