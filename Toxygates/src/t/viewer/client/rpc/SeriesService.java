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

import otgviewer.shared.MatchResult;
import otgviewer.shared.RankRule;
import otgviewer.shared.Series;
import t.common.shared.Dataset;
import t.common.shared.SampleClass;
import t.viewer.shared.NoSuchProbeException;
import t.viewer.shared.ServerError;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service for retrieving averaged time series and for ranking compounds.
 * 
 * @author johan
 * 
 */
@RemoteServiceRelativePath("series")
public interface SeriesService extends RemoteService {

  /**
   * Rank all available compounds for the given data filter according to the supplied rules.
   * 
   * @param filter
   * @param rules
   * @return
   * @throws NoSuchProbeException
   */
  public MatchResult[] rankedCompounds(Dataset[] ds, SampleClass sc, RankRule[] rules)
      throws ServerError;

  /**
   * Obtain a single time series or dose series.
   * 
   * @param filter
   * @param probe Must be a probe id.
   * @param timeDose A fixed time, or a fixed dose.
   * @param compound
   * @return
   */
  public Series getSingleSeries(SampleClass sc, String probe, String timeDose, String compound)
      throws ServerError;

  /**
   * Obtain a number of time series or dose series.
   * 
   * @param filter Must be specified.
   * @param probes Must be specified. Can be genes/proteins/probe ids.
   * @param timeDose A fixed time, or a fixed dose. Can optionally be null (no constraint).
   * @param compound Can optionally be null (no constraint). If this is null, timeDose must be null.
   * @return
   */
  public List<Series> getSeries(SampleClass sc, String[] probes, String timeDose, String[] compounds)
      throws ServerError;

  /**
   * Obtain the standard time points for a given group of series (identified by a representative
   * member)
   * 
   * @return
   */
  public String[] expectedTimes(Series s) throws ServerError;

}
