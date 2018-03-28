package t.intermine

import t.sparql.Platforms
import t.sparql.secondary.Gene
import t.sparql.Probes
import org.intermine.webservice.client.lists.ItemList
import org.intermine.webservice.client.core.ServiceFactory
import org.intermine.webservice.client.services.ListService
import org.intermine.webservice.client.core.ContentType
import org.json.JSONObject

/**
 * Connects to an Intermine data warehouse and obtains data.
 */
class Connector(val appName: String, val serviceUrl: String) {

  def serviceFactory = new ServiceFactory(serviceUrl)

  def getSessionToken(): String = {
    println(s"Connect to $appName")
    val sf = serviceFactory
    val s = sf.getService("session", appName)
    val r = s.createGetRequest(s.getUrl, ContentType.APPLICATION_JSON)
    val con = s.executeRequest(r)
    val rs = con.getResponseBodyAsString
    val obj = new JSONObject(rs)
    if (!obj.getBoolean("wasSuccessful")) {
      throw new Exception(s"Unable to get a session token from the Intermine server $serviceUrl")
    }
    val token = obj.getString("token")
    println(s"Opened intermine session: $token")
    con.close()
    token
  }
}

/**
 * Basic query support for Intermine.
 */
class Query(connector: Connector) {
  protected val serviceFactory = connector.serviceFactory
  protected val model = serviceFactory.getModel

  protected val token = connector.getSessionToken()
  protected val queryService = serviceFactory.getQueryService
  queryService.setAuthentication(token)

}