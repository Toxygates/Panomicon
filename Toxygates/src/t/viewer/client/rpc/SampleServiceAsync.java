package t.viewer.client.rpc;

import java.util.List;

import t.common.shared.*;
import t.common.shared.sample.*;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.SampleLike;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SampleServiceAsync {

  void chooseDatasets(Dataset[] enabled, AsyncCallback<SampleClass[]> callback);

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

  void annotations(Sample[] samples, Attribute[] attributes, AsyncCallback<Annotation[]> callback);

  void annotations(HasSamples<Sample> column, boolean importantOnly,
      AsyncCallback<Annotation[]> callback);
  
  void prepareAnnotationCSVDownload(HasSamples<Sample> column, 
      AsyncCallback<String> callback);

  void sampleSearch(SampleClass sampleClass, MatchCondition condition, int maxResults,
      AsyncCallback<RequestResult<Pair<Sample, Pair<Unit, Unit>>>> callback);

  void unitSearch(SampleClass sampleClass, MatchCondition condition, int maxResults,
      AsyncCallback<RequestResult<Pair<Unit, Unit>>> callback);

  void prepareCSVDownload(SampleLike[] samples, Attribute[] attributes,
      AsyncCallback<String> callback);
}
