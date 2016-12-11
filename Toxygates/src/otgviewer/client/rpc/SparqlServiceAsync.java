package otgviewer.client.rpc;

import otgviewer.shared.Pathology;
import t.common.shared.SampleClass;
import t.common.shared.sample.SampleColumn;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync extends t.viewer.client.rpc.SparqlServiceAsync {

  void pathologies(SampleColumn column, AsyncCallback<Pathology[]> callback);

  void probesTargetedByCompound(SampleClass sc, String compound, String service,
      boolean homologous, AsyncCallback<String[]> callback);


}
