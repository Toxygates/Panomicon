package t.viewer.client.rpc;

import java.util.List;

import t.common.shared.AType;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.sample.Sample;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProbeServiceAsync {

  void geneSyms(String[] probes, AsyncCallback<String[][]> callback);

  void filterProbesByGroup(String[] probes, List<Sample> samples, AsyncCallback<String[]> callback);

  void geneSuggestions(SampleClass sc, String partialName, AsyncCallback<String[]> callback);

  void identifiersToProbes(String[] identifiers, boolean precise, boolean quick,
      boolean titlePatternMatch, List<Sample> samples, AsyncCallback<String[]> callback);

  void keywordSuggestions(String partialName, int maxSize,
      AsyncCallback<Pair<String, AType>[]> callback);

}
