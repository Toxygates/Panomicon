package otgviewer.server.rpc

import t.viewer.server.rpc.TServiceServlet
import otg.Factory
import otg.Context
import t.viewer.server.Configuration
import otgviewer.shared.OTGSchema
import otg.OTGBConfig

trait OTGServiceServlet extends TServiceServlet {
  override protected def context: Context = _context
  override protected def factory: Factory = _factory
  
  protected var _context: Context = _ 
  protected var _factory: Factory = _

  override abstract def localInit(config: Configuration) {
    _factory = makeFactory()
    _context = _factory.context(config.tsConfig, config.dataConfig)
    super.localInit(config)
  }
  
  protected def makeFactory(): Factory = new Factory
  
  override protected def baseConfig: OTGBConfig = context.config
  
  protected val schema: OTGSchema = new OTGSchema()

}