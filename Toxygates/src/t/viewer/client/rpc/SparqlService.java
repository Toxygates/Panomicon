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

import javax.annotation.Nullable;

import t.common.shared.AType;
import t.common.shared.SampleClass;
import t.common.shared.sample.Sample;
import t.viewer.shared.AppInfo;
import t.viewer.shared.Association;
import t.viewer.shared.TimeoutException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service for obtaining data from SPARQL endpoints. These can be local or remote. All methods in
 * this service use their arguments to constrain the result that is being returned.
 * 
 * TODO this API is too big, try to make it smaller
 * 
 * @author johan
 * 
 */
@RemoteServiceRelativePath("sparql")
public interface SparqlService extends RemoteService, SampleService, ProbeService {

  /**
   * Obtain general application info. 
   * TODO migrate one-time mandatory data retrival to this object
   * to make the API smaller and reduce the number of calls
   * 
   * @return
   * @throws TimeoutException
   */
  public AppInfo appInfo(@Nullable String userDataKey) throws TimeoutException;

 
  /**
   * Obtain pathway names matching the pattern (partial name)
   * 
   * @param pattern
   * @return
   */
  public String[] pathways(SampleClass sc, String pattern) throws TimeoutException;

  /**
   * Obtain filtered probes that belong to the named pathway.
   * 
   * @param pathway
   * @param samples If null, all probes will be obtained.
   * @return
   */
  public String[] probesForPathway(SampleClass sc, String pathway, @Nullable List<Sample> samples)
      throws TimeoutException;

  /**
   * Obtain GO terms matching the given pattern (partial name)
   * 
   * @param pattern
   * @return
   */
  public String[] goTerms(String pattern) throws TimeoutException;

  /**
   * Obtain probes for a given GO term (fully named)
   * 
   * @param goTerm
   * @return
   */
  public String[] probesForGoTerm(String goTerm) throws TimeoutException;

  /**
   * Obtain filtered probes for a given GO term (fully named)
   * 
   * @param goTerm
   * @return
   */
  public String[] probesForGoTerm(String goTerm, @Nullable List<Sample> samples)
      throws TimeoutException;

 

  /**
   * Obtain associations -- the "dynamic columns" on the data screen.
   * 
   * @param types the association types to get.
   * @param filter
   * @param probes
   * @return
   */
  public Association[] associations(SampleClass sc, AType[] types, String[] probes)
      throws TimeoutException;

}
