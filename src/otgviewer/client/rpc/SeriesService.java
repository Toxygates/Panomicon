package otgviewer.client.rpc;

import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.MatchResult;
import otgviewer.shared.NoSuchProbeException;
import otgviewer.shared.RankRule;
import otgviewer.shared.Series;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service for retrieving averaged time series and for ranking compounds. 
 * @author johan
 *
 */
@RemoteServiceRelativePath("series")
public interface SeriesService extends RemoteService {
	
	/**
	 * Rank all available compounds for the given data filter according to the supplied rules.
	 * @param filter
	 * @param rules
	 * @return
	 * @throws NoSuchProbeException
	 */
	public MatchResult[] rankedCompounds(DataFilter filter, RankRule[] rules) throws NoSuchProbeException;
	
	/**
	 * Obtain a single time series or dose series.
	 * @param filter
	 * @param probe Must be a probe id.
	 * @param timeDose A fixed time, or a fixed dose.
	 * @param compound 
	 * @return
	 */
	public Series getSingleSeries(DataFilter filter, String probe, String timeDose, String compound);
	
	/**
	 * Obtain a number of time series or dose series.
	 * @param filter Must be specified.
	 * @param probes Must be specified. Can be genes/proteins/probe ids.
	 * @param timeDose A fixed time, or a fixed dose. Can optionally be null (no constraint).
	 * @param compound Can optionally be null (no constraint). If this is null, timeDose must be null.
	 * @return
	 */
	public List<Series> getSeries(DataFilter filter, String[] probes, String timeDose, String[] compounds);
	
}
