package t.clustering.client;

import java.util.List;

import t.clustering.shared.Algorithm;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ClusteringServiceAsync<C, R> {

  void prepareHeatmap(List<C> columns, List<R> rows, Algorithm algorithm,
      AsyncCallback<String> callback);

}
