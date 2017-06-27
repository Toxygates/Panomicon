package t.viewer.client;

public class Analytics {

  public static final String URL_PREFIX = "toxygates.html/";

  public static final String CATEGORY_ANALYSIS = "Analysis";
  public static final String CATEGORY_TABLE = "Table";
  public static final String CATEGORY_VISUALIZATION = "Visualization";
  public static final String CATEGORY_GENE_SET = "Gene set";
  public static final String CATEGORY_GENERAL = "General";

  public static final String ACTION_DISPLAY_CHARTS = "Display charts";
  public static final String ACTION_PAGE_CHANGE = "Page change";
  public static final String ACTION_COMPOUND_RANKING = "Compound ranking";
  public static final String ACTION_COMPOUND_RANKING_CHARTS = "Compound ranking charts";
  public static final String ACTION_SAVE_SAMPLE_GROUP = "Save sample group";
  public static final String ACTION_DISPLAY_OPTIONAL_COLUMN = "Display optional column";
  public static final String ACTION_PERFORM_ENRICHMENT = "Perform enrichment";
  public static final String ACTION_PERFORM_CLUSTERING = "Perform clustering";
  public static final String ACTION_MAGNIFY_CHART = "Magnify chart";
  public static final String ACTION_FILTER_COLUMN = "Enable/modify column filter";
  public static final String ACTION_PERFORM_UNIT_SEARCH = "Perform unit search";

  /*
   * Tracks a pageview with Google Analytics if the google analytics script has been loaded.
   * 
   * @param url the url to be tracked
   */
  public static native void trackPageView(String url) /*-{
		if ($wnd.ga) {
			$wnd.ga('set', 'page', url);
			$wnd.ga('send', 'pageview');
		}
		;
  }-*/;

  /*
   * Tracks an event with Google Analytics.
   * 
   * @param eventCategory
   * 
   * @param eventAction
   * 
   * @param eventLabel
   */
  public static native void trackEvent(String eventCategory, String eventAction,
      String eventLabel) /*-{
		if ($wnd.ga) {
			$wnd.ga('send', 'event', eventCategory, eventAction, eventLabel)
		}
		;
  }-*/;

  public static native void trackEvent(String eventCategory, String eventAction) /*-{
		if ($wnd.ga) {
			$wnd.ga('send', 'event', eventCategory, eventAction)
		}
		;
  }-*/;
}
