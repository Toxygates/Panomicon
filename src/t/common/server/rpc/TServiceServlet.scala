package t.common.server.rpc

import com.google.gwt.user.server.rpc.RemoteServiceServlet

import javax.servlet.ServletConfig
import javax.servlet.ServletException
import t.BaseConfig
import t.Context
import t.Factory
import t.viewer.server.Configuration
import t.common.shared.DataSchema

abstract class TServiceServlet extends RemoteServiceServlet {
  protected def context: Context
  protected def factory: Factory
  
  @throws(classOf[ServletException])
  override def init(config: ServletConfig) {
    super.init(config)        
    localInit(Configuration.fromServletConfig(config))    
  }
  
  //This method should initialise Factory and Context.
  //Public for test purposes 
  def localInit(config: Configuration): Unit = {}
  
  protected def makeFactory(): Factory
  
  protected def baseConfig = context.config  
  
  protected def schema: DataSchema
}