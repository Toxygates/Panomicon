package t.viewer.client.rpc;

import java.util.List;

import otgviewer.shared.Annotation;
import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SampleServiceAsync {

  public void chooseDatasets(Dataset[] enabled, AsyncCallback<Void> callback);

  @Deprecated
  public void sampleClasses(AsyncCallback<SampleClass[]> callback);

  public void parameterValues(Dataset[] ds, SampleClass sc, String parameter,
      AsyncCallback<String[]> callback);

  public void parameterValues(SampleClass sc, String parameter, AsyncCallback<String[]> callback);

  public void parameterValues(SampleClass[] scs, String parameter, AsyncCallback<String[]> callback);

  public void samplesById(String[] ids, AsyncCallback<Sample[]> callback);

  public void samplesById(List<String[]> ids, AsyncCallback<List<Sample[]>> callback);

  public void samples(SampleClass sc, AsyncCallback<Sample[]> callback);

  public void samples(SampleClass sc, String param, String[] paramValues,
      AsyncCallback<Sample[]> callback);

  public void samples(SampleClass[] scs, String param, String[] paramValues,
      AsyncCallback<Sample[]> callback);

  public void units(SampleClass sc, String param, String[] paramValues,
      AsyncCallback<Pair<Unit, Unit>[]> callback);

  public void units(SampleClass[] sc, String param, String[] paramValues,
      AsyncCallback<Pair<Unit, Unit>[]> callback);

  void annotations(Sample barcode, AsyncCallback<Annotation> callback);

  void annotations(HasSamples<Sample> column, boolean importantOnly,
      AsyncCallback<Annotation[]> callback);
}
