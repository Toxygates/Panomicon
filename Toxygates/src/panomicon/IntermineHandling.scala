package panomicon

import t.Context
import t.server.viewer.intermine.{IntermineConnector}
import t.shared.viewer.intermine.IntermineInstance

class IntermineHandling(context: Context) {

  lazy val instance = {
    new IntermineInstance(
      //Title displayed to the user
      System.getenv("T_INTERMINE_NAME"),
      //Internal ID string
      System.getenv("T_INTERMINE_ID"),
      //URL to application
      System.getenv("T_INTERMINE_URL"))
  }

  lazy val connector = new IntermineConnector(instance, context.platformRegistry)
}
