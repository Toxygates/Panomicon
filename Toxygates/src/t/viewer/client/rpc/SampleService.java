package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.shared.Annotation;
import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.viewer.shared.TimeoutException;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * A service that provides information about samples, datasets, and 
 * related objects.
 */
public interface SampleService extends RemoteService {
  
  public void chooseDatasets(Dataset[] enabled) throws TimeoutException;
  
  /**
   * Obtain all sample classes in the triple store
   * 
   * @return
   */
  @Deprecated
  public SampleClass[] sampleClasses() throws TimeoutException;
  
  public String[] parameterValues(Dataset[] ds, SampleClass sc, String parameter)
      throws TimeoutException;

  public String[] parameterValues(SampleClass sc, String parameter) throws TimeoutException;

  public String[] parameterValues(SampleClass[] scs, String parameter) throws TimeoutException;
  
  /**
   * Obtain samples (fully populated with metadata) from given IDs
   * 
   * @param ids
   * @return
   * @throws TimeoutException
   */
  public Sample[] samplesById(String[] ids) throws TimeoutException;

  /**
   * Obtain samples, fully populated with metadata, from given IDs.
   * Convenience function that keeps samples grouped.
   * @param ids
   * @return
   * @throws TimeoutException
   */
  public List<Sample[]> samplesById(List<String[]> ids) throws TimeoutException;
  
  /**
   * Obtain samples for a given sample class.
   * 
   * @param sc
   * @return
   */
  public Sample[] samples(SampleClass sc) throws TimeoutException;

  /**
   * Obtain samples with a filter on one parameter.
   * 
   * @param sc
   * @return
   */
  public Sample[] samples(SampleClass sc, String param, String[] paramValues)
      throws TimeoutException;

  public Sample[] samples(SampleClass[] scs, String param, String[] paramValues)
      throws TimeoutException;
  
  /**
   * Obtain units that are populated with the samples that belong to them, with a filter on one
   * parameter.
   * 
   * @param sc
   * @param
   * @return Pairs of units, where the first is treated samples and the second the corresponding
   *         control samples, or null if there are none.
   */
  public Pair<Unit, Unit>[] units(SampleClass sc, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  public Pair<Unit, Unit>[] units(SampleClass[] scs, String param, @Nullable String[] paramValues)
      throws TimeoutException;


  /**
   * Annotations are experiment-associated information such as dose, time, biochemical data etc.
   * This method obtains them for a single sample.
   * 
   * @param barcode
   * @return
   */
  public Annotation annotations(Sample barcode) throws TimeoutException;

  /**
   * Obtain annotations for a set of samples
   * 
   * @param column
   * @param importantOnly If true, a smaller set of core annotations will be obtained. If false, all
   *        annotations will be obtained.
   * @return
   */
  public Annotation[] annotations(HasSamples<Sample> column, boolean importantOnly)
      throws TimeoutException;


}
