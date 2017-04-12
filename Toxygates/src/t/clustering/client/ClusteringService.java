package t.clustering.client;

import java.util.List;

import t.clustering.shared.Algorithm;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * A clustering service to generate heatmaps.
 *
 * @param <C> Identifies columns.
 * @param <R> Identifies rows.
 */
public interface ClusteringService<C, R> extends RemoteService {

  
  /**
   * Perform a clustering for the named columns and rows, and return the result as a 
   * JSON String.
   * @param columns
   * @param rows
   * @param algorithm
   * @return
   */
  String prepareHeatmap(List<C> columns, List<R> rows, Algorithm algorithm);
}
