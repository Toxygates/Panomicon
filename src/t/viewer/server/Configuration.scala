package t.viewer.server

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
    
    def p(x: String) = servletContext.getInitParameter(x)
    
    /**
     * These parameters are read from <context-param> tags in WEB-INF/web.xml.
     */
    new Configuration(p("repositoryName"),
      p("dataDir"),
      p("csvDir"),
      p("csvUrlBase"),      
      parseAClass(p("applicationClass")),
      p("repositoryURL"),
      p("updateURL"),
      p("repositoryUser"),
      p("repositoryPassword"),
      p("instanceName"),
      p("webappHomeDir"))
  } 
  
  def parseAClass(v: String): ApplicationClass = {
    if (v == null) {
      Toxygates
    } else {
      ApplicationClass.withName(v)
    }
  } 
}

class Configuration(val repositoryName: String, 
    val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String,         
    @Deprecated val applicationClass: ApplicationClass = Toxygates,
    val repositoryUrl: String = null,
    val updateUrl: String = null,
    val repositoryUser: String = null,
    val repositoryPass: String = null,
    val instanceName: String = null,
    val webappHomeDir: String = null) {
  
  def this(owlimRepository: String, toxygatesHome:String, foldsDBVersion: Int) = 
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"), 
        System.getProperty("otg.csvUrlBase"))

  def tsConfig = TriplestoreConfig(repositoryUrl, updateUrl,
    repositoryUser, repositoryPass, repositoryName)
  def dataConfig = DataConfig(toxygatesHomeDir)
    
  def context(bc: BaseConfig) = new OTGContext(bc)  
}