package otgviewer.client.rpc;

import otgviewer.shared.Pathology;
import t.common.shared.SampleClass;
import t.common.shared.sample.SampleColumn;
import t.viewer.shared.TimeoutException;

public interface SparqlService extends t.viewer.client.rpc.SparqlService {
  /**
   * Obtain pathologies for a set of samples
   * 
   * @param column
   * @return
   */
  Pathology[] pathologies(SampleColumn column) throws TimeoutException;

  /**
   * Obtain probes that correspond to proteins targeted by the named compound.
   * 
   * @param compound
   * @param service Service to use for lookup (currently DrugBank or CHEMBL) (TODO it might be
   *        better to use an enum)
   * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
   * @return
   */
  String[] probesTargetedByCompound(SampleClass sc, String compound, String service,
      boolean homologous) throws TimeoutException;

}
