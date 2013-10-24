package otgviewer.server

import javax.servlet.ServletConfig

object Configuration {
  def fromServletConfig(config: ServletConfig) =    
    new Configuration(config.getInitParameter("owlimRepositoryName"), 
        config.getInitParameter("toxygatesHomeDir"),
        config.getInitParameter("csvDir"),
        config.getInitParameter("csvUrlBase"))
}

// TODO: add CSV parameters
class Configuration(val owlimRepositoryName: String, val toxygatesHomeDir: String,
    val csvDirectory: String, val csvUrlBase: String) {
  
  def this(owlimRepository: String, toxygatesHome:String) = 
    this(owlimRepository, toxygatesHome, System.getProperty("otg.csvDir"), 
        System.getProperty("otg.csvUrlBase"))
}