package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.user.client.rpc.RemoteService;

import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.viewer.shared.TimeoutException;

/**
 * A service that provides information about samples, datasets, and 
 * related objects.
 */
public interface SampleService extends RemoteService {
  
  void chooseDatasets(Dataset[] enabled) throws TimeoutException;
  
  /**
   * Obtain all sample classes in the triple store
   * 
   * @return
   */
  @Deprecated
  SampleClass[] sampleClasses() throws TimeoutException;
  
  String[] parameterValues(Dataset[] ds, SampleClass sc, String parameter)
      throws TimeoutException;

  String[] parameterValues(SampleClass sc, String parameter) throws TimeoutException;

  String[] parameterValues(SampleClass[] scs, String parameter) throws TimeoutException;
  
  /**
   * Obtain samples (fully populated with metadata) from given IDs
   * 
   * @param ids
   * @return
   * @throws TimeoutException
   */
  Sample[] samplesById(String[] ids) throws TimeoutException;

  /**
   * Obtain samples, fully populated with metadata, from given IDs.
   * Convenience function that keeps samples grouped.
   * @param ids
   * @return
   * @throws TimeoutException
   */
  List<Sample[]> samplesById(List<String[]> ids) throws TimeoutException;
  
  /**
   * Obtain samples for a given sample class.
   * 
   * @param sc
   * @return
   */
  Sample[] samples(SampleClass sc) throws TimeoutException;

  /**
   * Obtain samples with a filter on one parameter.
   * 
   * @param sc
   * @return
   */
  Sample[] samples(SampleClass sc, String param, String[] paramValues)
      throws TimeoutException;

  Sample[] samples(SampleClass[] scs, String param, String[] paramValues)
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
  Pair<Unit, Unit>[] units(SampleClass sc, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  Pair<Unit, Unit>[] units(SampleClass[] scs, String param, @Nullable String[] paramValues)
      throws TimeoutException;


  /**
   * Annotations are experiment-associated information such as dose, time, biochemical data etc.
   * This method obtains them for a single sample.
   * 
   * @param barcode
   * @return
   */
  Annotation annotations(Sample barcode) throws TimeoutException;

  /**
   * Obtain annotations for a set of samples
   * 
   * @param column
   * @param importantOnly If true, a smaller set of core annotations will be obtained. If false, all
   *        annotations will be obtained.
   * @return
   */
  Annotation[] annotations(HasSamples<Sample> column, boolean importantOnly)
      throws TimeoutException;

  /**
   * Prepare a CSV file with annotation information for download.
   * @param column
   * @return The URL of the downloadable file.
   * @throws TimeoutException
   */
  String prepareAnnotationCSVDownload(HasSamples<Sample> column) throws TimeoutException;
  
  Sample[] sampleSearch(SampleClass sampleClass, MatchCondition condition) throws TimeoutException;

  Unit[] unitSearch(SampleClass sampleClass, MatchCondition condition)
      throws TimeoutException;

  String prepareUnitCSVDownload(Unit[] units, String[] parameterNames) throws TimeoutException;
}
