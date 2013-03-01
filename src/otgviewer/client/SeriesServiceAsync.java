package otgviewer.client;

import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.MatchResult;
import otgviewer.shared.RankRule;
import otgviewer.shared.Series;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SeriesServiceAsync {
	public void rankedCompounds(DataFilter filter, RankRule[] rules, AsyncCallback<MatchResult[]> callback);

	public void getSingleSeries(DataFilter filter, String probe,
			String timeDose, String compound, AsyncCallback<Series> callback);

	public void getSeries(DataFilter filter, String[] probes, String timeDose,
			String[] compounds, AsyncCallback<List<Series>> callback);
}
