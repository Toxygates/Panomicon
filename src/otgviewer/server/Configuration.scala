package otgviewer.server

import ApplicationClass.ApplicationClass
import ApplicationClass.Toxygates
import javax.servlet.ServletConfig
import otg.OTGContext
import t.TriplestoreConfig
import t.DataConfig
import t.BaseConfig

object Configuration {
  /**
   * Create a new Configuration from the ServletConfig.
   */
  def fromServletConfig(config: ServletConfig): Configuration = {
    val servletContext = config.getServletContext()
    
    /**
     * These parameters are read from <context-param> tags in WEB-INF/web.xml.
     */
    new Configuration(servletContext.getInitParameter("repositoryName"),
      servletContext.getInitParameter("dataDir"),
      servletContext.getInitParameter("csvDir"),
      servletContext.getInitParameter("csvUrlBase"),      
      parseAClass(servletContext.getInitParameter("applicationClass")),
      servletContext.getInitParameter("repositoryURL"),
      servletContext.getInitParameter("updateURL"))
  }
  
  def parseAClass(v: String): ApplicationClass = {
    if (v == null) {
      Toxygates
    } else {
      ApplicationClass.withName(v)
    }
  } 
}

class Configuration(val owlimRepositoryName: String, 
    val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String,     
    val applicationClass: ApplicationClass = Toxygates,
    val repositoryUrl: String = null,
    val updateUrl: String = null) {
  
  def this(owlimRepository: String, toxygatesHome:String, foldsDBVersion: Int) = 
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"), 
        System.getProperty("otg.csvUrlBase"))
  
  lazy val context = 
    new OTGContext(Some(toxygatesHomeDir), Some(owlimRepositoryName),
        None)
  
  def baseConfig = {
    val tsConfig = TriplestoreConfig(repositoryUrl, updateUrl, 
        "toxygates", "toxygates", owlimRepositoryName)
    val dataConfig = DataConfig(toxygatesHomeDir)
    BaseConfig(tsConfig, dataConfig)
  }    
}