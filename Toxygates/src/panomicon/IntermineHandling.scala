package panomicon

import panomicon.json.GeneList
import t.Context
import t.server.viewer.intermine.IntermineConnector
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

  def importLists(user: String, pass: String): Iterable[GeneList] =
    connector.importLists(user, pass).map(l => GeneList(l._2, l._1))

  def exportLists(user: String, pass: String, lists: Iterable[GeneList], replace: Boolean): Unit =
    connector.exportLists(user, pass, lists.map(l => (l.items, l.name)), replace)

}
