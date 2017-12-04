package otgviewer.client.rpc;

import otgviewer.shared.Pathology;
import t.common.shared.sample.SampleColumn;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SampleServiceAsync extends t.viewer.client.rpc.SampleServiceAsync {

  void pathologies(SampleColumn column, AsyncCallback<Pathology[]> callback);

}
