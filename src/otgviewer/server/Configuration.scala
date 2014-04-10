package otgviewer.server

import ApplicationClass.ApplicationClass
import ApplicationClass.Toxygates
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
      servletContext.getInitParameter("csvUrlBase"),
      parseVersion(servletContext.getInitParameter("foldsDBVersion")),
      parseAClass(servletContext.getInitParameter("applicationClass")))
  }
  
  def parseAClass(v: String): ApplicationClass = {
    if (v == null) {
      Toxygates
    } else {
      ApplicationClass.withName(v)
    }
  }
  def parseVersion(v: String): Int = {
    if (v == null) {
      1
    } else if (v == "2") {
      2
    } else {
      1
    }
  }
}

class Configuration(val owlimRepositoryName: String, 
    val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String, 
    val foldsDBVersion: Int,
    val applicationClass: ApplicationClass = Toxygates) {
  
  def this(owlimRepository: String, toxygatesHome:String, foldsDBVersion: Int) = 
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"), 
        System.getProperty("otg.csvUrlBase"), foldsDBVersion)
  
  def usePrefixIdMap: Boolean = (foldsDBVersion == 1)
        
  lazy val context = 
    new OTGContext(Some(toxygatesHomeDir), Some(owlimRepositoryName),
        None, usePrefixIdMap) 
}