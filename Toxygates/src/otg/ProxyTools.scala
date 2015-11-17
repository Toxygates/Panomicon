package otg

import com.gdevelop.gwt.syncrpc.SyncProxy
import com.gdevelop.gwt.syncrpc.ProxySettings

/**
 * @author johan
 */
object ProxyTools {

  /**
   * Obtain a proxy.
   * SyncProxy#setBaseUrl must be called prior to using this method
   */
   def getProxy[T](cls: Class[T], relativePath: String): T = {
    val ps = new ProxySettings()
    ps.setRemoteServiceRelativePath(relativePath)
    SyncProxy.createProxy(cls, ps)
  }
}
