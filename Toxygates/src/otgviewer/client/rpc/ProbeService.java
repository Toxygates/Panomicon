package otgviewer.client.rpc;

import t.model.SampleClass;
import t.viewer.shared.TimeoutException;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("probe")
public interface ProbeService extends t.viewer.client.rpc.ProbeService {

  /**
   * Obtain probes that correspond to proteins targeted by the named compound.
   * 
   * @param compound
   * @param service Service to use for lookup (currently DrugBank or CHEMBL) 
   * (TODO it might be better to use an enum)
   * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
   * @return
   */
  String[] probesTargetedByCompound(SampleClass sc, String compound, String service,
      boolean homologous) throws TimeoutException;

}
