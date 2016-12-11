/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.rpc;

import java.util.List;

import t.common.shared.AType;
import t.common.shared.SampleClass;
import t.common.shared.sample.Sample;
import t.viewer.shared.AppInfo;
import t.viewer.shared.Association;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync extends SampleServiceAsync, ProbeServiceAsync {

  public void appInfo(String userDataKey, AsyncCallback<AppInfo> callback);

  public void pathways(SampleClass sc, String pattern, AsyncCallback<String[]> callback);

  public void probesForPathway(SampleClass sc, String pathway, List<Sample> samples,
      AsyncCallback<String[]> callback);

  public void geneSyms(String[] probes, AsyncCallback<String[][]> callback);

  public void geneSuggestions(SampleClass sc, String partialName, AsyncCallback<String[]> callback);

  public void goTerms(String pattern, AsyncCallback<String[]> callback);

  @Deprecated
  public void probesForGoTerm(String term, AsyncCallback<String[]> callback);

  public void probesForGoTerm(String pattern, List<Sample> samples, AsyncCallback<String[]> callback);

  public void associations(SampleClass sc, AType[] types, String[] probes,
      AsyncCallback<Association[]> callback);
}
