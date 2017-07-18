package t.viewer.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;

public interface SampleServiceAsync {

  void chooseDatasets(Dataset[] enabled, AsyncCallback<Void> callback);

  @Deprecated
  void sampleClasses(AsyncCallback<SampleClass[]> callback);

  void parameterValues(Dataset[] ds, SampleClass sc, String parameter,
      AsyncCallback<String[]> callback);

  void parameterValues(SampleClass sc, String parameter, AsyncCallback<String[]> callback);

  void parameterValues(SampleClass[] scs, String parameter, AsyncCallback<String[]> callback);

  void samplesById(String[] ids, AsyncCallback<Sample[]> callback);

  void samplesById(List<String[]> ids, AsyncCallback<List<Sample[]>> callback);

  void samples(SampleClass sc, AsyncCallback<Sample[]> callback);

  void samples(SampleClass sc, String param, String[] paramValues, AsyncCallback<Sample[]> callback);

  void samples(SampleClass[] scs, String param, String[] paramValues,
      AsyncCallback<Sample[]> callback);

  void units(SampleClass sc, String param, String[] paramValues,
      AsyncCallback<Pair<Unit, Unit>[]> callback);

  void units(SampleClass[] sc, String param, String[] paramValues,
      AsyncCallback<Pair<Unit, Unit>[]> callback);

  void annotations(Sample barcode, AsyncCallback<Annotation> callback);

  void annotations(HasSamples<Sample> column, boolean importantOnly,
      AsyncCallback<Annotation[]> callback);
  
  void prepareAnnotationCSVDownload(HasSamples<Sample> column, 
      AsyncCallback<String> callback);

  void sampleSearch(SampleClass sampleClass, MatchCondition condition,
      AsyncCallback<Sample[]> callback);

  void unitSearch(SampleClass sampleClass, MatchCondition condition,
      AsyncCallback<Unit[]> callback);

  void prepareUnitCSVDownload(Unit[] units, String[] parameterNames,
      AsyncCallback<String> callback);
}
