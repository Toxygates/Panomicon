package otgviewer.server

import javax.servlet.ServletConfig

object Configuration {
  def fromServletConfig(config: ServletConfig) = 
    new Configuration(config.getInitParameter("owlimRepositoryName"), 
        config.getInitParameter("toxygatesHomeDir"))
}

class Configuration(val owlimRepositoryName: String, val toxygatesHomeDir: String)