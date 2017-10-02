package t.viewer.client.rpc;

import java.util.List;

import t.common.shared.AType;
import t.common.shared.Pair;
import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.viewer.shared.AppInfo;
import t.viewer.shared.Association;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProbeServiceAsync {
  void appInfo(String userDataKey, AsyncCallback<AppInfo> callback);

  void geneSyms(String[] probes, AsyncCallback<String[][]> callback);

  void filterProbesByGroup(String[] probes, List<Sample> samples, AsyncCallback<String[]> callback);

  void geneSuggestions(SampleClass sc, String partialName, AsyncCallback<String[]> callback);

  void identifiersToProbes(String[] identifiers, boolean precise, boolean quick,
      boolean titlePatternMatch, List<Sample> samples, AsyncCallback<String[]> callback);

  void keywordSuggestions(String partialName, int maxSize,
      AsyncCallback<Pair<String, AType>[]> callback);
  
  void pathways(String pattern, AsyncCallback<String[]> callback);

  void probesForPathway(String pathway, List<Sample> samples, AsyncCallback<String[]> callback);

  void goTerms(String pattern, AsyncCallback<String[]> callback);

  @Deprecated
  void probesForGoTerm(String term, AsyncCallback<String[]> callback);

  void probesForGoTerm(String pattern, List<Sample> samples, AsyncCallback<String[]> callback);

  void associations(SampleClass sc, AType[] types, String[] probes,
      AsyncCallback<Association[]> callback);

  void setMirnaSources(MirnaSource[] sources, AsyncCallback<Void> callback);

}
