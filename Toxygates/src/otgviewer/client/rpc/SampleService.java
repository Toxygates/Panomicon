package otgviewer.client.rpc;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import otgviewer.shared.Pathology;
import t.common.shared.sample.SampleColumn;
import t.viewer.shared.TimeoutException;

@RemoteServiceRelativePath("sample")
public interface SampleService extends t.viewer.client.rpc.SampleService {
  /**
   * Obtain pathologies for a set of samples
   * 
   * @param column
   * @return
   */
  Pathology[] pathologies(SampleColumn column) throws TimeoutException;
}
