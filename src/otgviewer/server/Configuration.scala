package otgviewer.server

import javax.servlet.ServletConfig

object Configuration {
  def fromServletConfig(config: ServletConfig) =    
    new Configuration(config.getInitParameter("owlimRepositoryName"), 
        config.getInitParameter("toxygatesHomeDir"))
}

// TODO: add CSV parameters
class Configuration(val owlimRepositoryName: String, val toxygatesHomeDir: String)