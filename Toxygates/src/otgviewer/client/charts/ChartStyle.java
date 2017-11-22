package otgviewer.client.charts;

import com.google.gwt.user.client.ui.HTML;

/**
 * Chart display options
 */
public class ChartStyle {
  public final boolean isSeries;
  public final int width;
  public final HTML downloadLink;
  public final boolean bigMode;
  
  public ChartStyle(int width, boolean isSeries,
                    final HTML downloadLink, boolean bigMode) {
    this.isSeries = isSeries;
    this.width = width;
    this.downloadLink = downloadLink;
    this.bigMode = bigMode;
  }
  
  public ChartStyle withWidth(int width) {
    return new ChartStyle(width, isSeries, downloadLink, bigMode);
  }
  
  public ChartStyle withBigMode(boolean bigMode) {
    return new ChartStyle(width, isSeries, downloadLink, bigMode);
  }
  
  public ChartStyle withDownloadLink(HTML downloadLink) {
    return new ChartStyle(width, isSeries, downloadLink, bigMode);
  }
}
