package t.viewer.client;

public class Analytics {

  public static final String URL_PREFIX = "toxygates.html/";

  public static final String CATEGORY_ANALYSIS = "Analysis";
  public static final String ACTION_COMPOUND_RANKING = "Compound ranking";
  public static final String ACTION_COMPOUND_RANKING_CHARTS = "Compound ranking charts";
  public static final String ACTION_PERFORM_ENRICHMENT = "Perform enrichment";
  public static final String ACTION_PERFORM_UNIT_SEARCH = "Perform unit search";
  public static final String ACTION_ADD_COMPARISON_COLUMN = "Add two-group comparison column";
  public static final String LABEL_T_TEST = "T-test";
  public static final String LABEL_U_TEST = "U-test";
  public static final String LABEL_FOLD_CHANGE_DIFFERENCE = "Fold-change difference";
  public static final String ACTION_SHOW_HEAT_MAP = "Show heat map";
  public static final String ACTION_ENRICH_CLUSTERS = "Enrich clusters";
  public static final String ACTION_SAVE_CLUSTERS = "Save clusters as gene set";

  public static final String CATEGORY_TABLE = "Table";
  public static final String ACTION_PAGE_CHANGE = "Render/change page";
  public static final String ACTION_DISPLAY_OPTIONAL_COLUMN = "Display optional column";
  public static final String ACTION_FILTER_COLUMN = "Enable/modify column filter";

  public static final String CATEGORY_VISUALIZATION = "Visualization";
  public static final String ACTION_DISPLAY_CHARTS = "Display charts";
  public static final String ACTION_MAGNIFY_CHART = "Magnify chart";
  public static final String ACTION_DISPLAY_MINI_HEATMAP = "Display mini heatmap";

  public static final String CATEGORY_GENE_SET = "Gene set";
  public static final String ACTION_CREATE_NEW_GENE_SET = "Create new gene set";
  public static final String ACTION_MODIFY_EXISTING_GENE_SET = "Modify existing gene set";
  public static final String ACTION_DELETE_GENE_SET = "Delete gene set";

  public static final String CATEGORY_GENERAL = "General";
  public static final String ACTION_SAVE_SAMPLE_GROUP = "Save sample group";

  public static final String CATEGORY_IMPORT_EXPORT = "Import/Export";
  public static final String ACTION_DOWNLOAD_SAMPLE_DETAILS = "Download sample details";


  public static final String ACTION_PERFORM_CLUSTERING = "Perform clustering"; // Not tracked yet

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