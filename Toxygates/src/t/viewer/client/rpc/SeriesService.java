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

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import t.shared.common.Dataset;
import t.shared.common.SeriesType;
import t.model.SampleClass;
import t.viewer.shared.MatchResult;
import t.viewer.shared.RankRule;
import t.viewer.shared.Series;

/**
 * A service for retrieving averaged time series and for ranking compounds.
 * 
 * @author johan
 * 
 */
@RemoteServiceRelativePath("series")
public interface SeriesService extends RemoteService {

  MatchResult[] rankedCompounds(SeriesType seriesType, Dataset[] ds, SampleClass sc,
                                RankRule[] rules);

  Series getSingleSeries(SeriesType seriesType, SampleClass sc, String probe, String timeDose,
                         String compound);

  List<Series> getSeries(SeriesType seriesType, SampleClass sc, String[] probes, String timeDose,
      String[] compounds);

  String[] expectedIndependentPoints(SeriesType seriesType, Series s);

}
