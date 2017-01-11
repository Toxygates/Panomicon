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

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SeriesServiceAsync {
  void rankedCompounds(Dataset[] ds, SampleClass sc, RankRule[] rules,
      AsyncCallback<MatchResult[]> callback);

  void getSingleSeries(SampleClass sc, String probe, String timeDose, String compound,
      AsyncCallback<Series> callback);

  void getSeries(SampleClass sc, String[] probes, String timeDose, String[] compounds,
      AsyncCallback<List<Series>> callback);

  void expectedTimes(Series s, AsyncCallback<String[]> callback);
}
