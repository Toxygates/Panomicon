package otgviewer.client.rpc;

import t.model.SampleClass;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProbeServiceAsync extends t.viewer.client.rpc.ProbeServiceAsync {

  void probesTargetedByCompound(SampleClass sc, String compound, String service,
      boolean homologous, AsyncCallback<String[]> callback);

}
