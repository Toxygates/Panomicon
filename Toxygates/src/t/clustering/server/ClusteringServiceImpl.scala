package t.clustering.server

import t.clustering.client.ClusteringService
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import t.clustering.shared.Algorithm
import java.util.{List => JList}
import scala.collection.JavaConversions._

abstract class ClusteringServiceImpl[C, R] extends RemoteServiceServlet with ClusteringService[C, R] {
  
  def prepareHeatmap(columns: JList[C], rows: JList[R],
    algorithm: Algorithm): String = {

    val data = clusteringData(columns, rows)
    
    val clust = new RClustering(data.userDir)
    
    clust.clustering(data.data.flatten, Array() ++ data.rowNames,
        Array() ++ data.colNames, 
        Array() ++ data.geneSymbols, algorithm)
  }
  
  protected def clusteringData(cols: JList[C], rows: JList[R]): ClusteringData
  
}