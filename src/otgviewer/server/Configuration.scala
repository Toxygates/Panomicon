package otgviewer.server

import javax.servlet.ServletConfig
import otg.OTGContext

object Configuration {
  /**
   * Create a new Configuration from the ServletConfig.
   */
  def fromServletConfig(config: ServletConfig): Configuration = {
    val servletContext = config.getServletContext()
    
    /**
     * These parameters are read from <context-param> tags in WEB-INF/web.xml.
     */
    new Configuration(servletContext.getInitParameter("owlimRepositoryName"),
      servletContext.getInitParameter("toxygatesHomeDir"),
      servletContext.getInitParameter("csvDir"),
      servletContext.getInitParameter("csvUrlBase"))
  }
       
}

class Configuration(val owlimRepositoryName: String, val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String) {
  
  def this(owlimRepository: String, toxygatesHome:String) = 
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"), 
        System.getProperty("otg.csvUrlBase"))
  
  lazy val context = 
    new OTGContext(Some(toxygatesHomeDir), Some(owlimRepositoryName)) 
}