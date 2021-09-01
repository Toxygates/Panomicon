/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import t.shared.common.AType;
import t.shared.common.Pair;
import t.shared.common.sample.Sample;
import t.model.SampleClass;
import t.shared.viewer.AppInfo;
import t.shared.viewer.Association;

import java.util.List;

public interface ProbeServiceAsync {
  void appInfo(String userDataKey, AsyncCallback<AppInfo> callback);

  void geneSyms(String[] probes, AsyncCallback<String[][]> callback);

  void geneSuggestions(SampleClass sc, String partialName,
      AsyncCallback<Pair<String, String>[]> callback);

  void identifiersToProbes(String[] identifiers, boolean precise, boolean quick,
      boolean titlePatternMatch, List<Sample> samples, AsyncCallback<String[]> callback);

  void keywordSuggestions(String partialName, int maxSize,
      AsyncCallback<Pair<String, AType>[]> callback);

  void probesForPathway(String pathway, List<Sample> samples, AsyncCallback<String[]> callback);

  void probesForGoTerm(String pattern, List<Sample> samples, AsyncCallback<String[]> callback);

  void associations(SampleClass sc, AType[] types, String[] probes,
      int sizeLimit, AsyncCallback<Association[]> callback);

  void probesTargetedByCompound(SampleClass sc, String compound, String service,
                                boolean homologous, AsyncCallback<String[]> callback);
}
