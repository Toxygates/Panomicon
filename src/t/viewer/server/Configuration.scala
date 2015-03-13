package t.viewer.server

import javax.servlet.ServletConfig
import otg.OTGContext
import t.TriplestoreConfig
import t.DataConfig
import t.BaseConfig
import t.db.MatrixContext
import t.Factory
import t.Context

object Configuration {
  /**
   * Create a new Configuration from the ServletConfig.
   */
  def fromServletConfig(config: ServletConfig) = {
    val servletContext = config.getServletContext()
    
    def p(x: String) = servletContext.getInitParameter(x)
    
    /**
     * These parameters are read from <context-param> tags in WEB-INF/web.xml.
     */
    new Configuration(p("repositoryName"),
      p("dataDir"),
      p("csvDir"),
      p("csvUrlBase"),      
      p("repositoryURL"),
      p("updateURL"),
      p("repositoryUser"),
      p("repositoryPassword"),
      p("instanceName"),
      p("webappHomeDir"),
      p("matrixDbOptions"))
  }    
}

/**
 * A bridge from ServletConfig to Context.
 */
class Configuration(val repositoryName: String, 
    val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String,         
    val repositoryUrl: String = null,
    val updateUrl: String = null,
    val repositoryUser: String = null,
    val repositoryPass: String = null,
    val instanceName: String = null,
    val webappHomeDir: String = null,
    val matrixDbOptions: String = null) {
  
  def this(owlimRepository: String, toxygatesHome:String, foldsDBVersion: Int) = 
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"), 
        System.getProperty("otg.csvUrlBase"))

  def tsConfig = TriplestoreConfig(repositoryUrl, updateUrl,
    repositoryUser, repositoryPass, repositoryName)
  def dataConfig = DataConfig(toxygatesHomeDir, matrixDbOptions)
  
  def context(f: Factory): Context = f.context(tsConfig, dataConfig)         
}