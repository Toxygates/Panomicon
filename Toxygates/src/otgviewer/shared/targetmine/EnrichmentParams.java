package otgviewer.shared.targetmine;

import java.io.Serializable;

@SuppressWarnings("serial")
public class EnrichmentParams implements Serializable {

  public EnrichmentWidget widget;
  public String filter; //parameter for the widget
  public double cutoff;
  public Correction correction;
  
  //GWT constructor
  public EnrichmentParams() {}
  
  public EnrichmentParams(EnrichmentWidget widget, String filter,
      double cutoff, Correction correction) {
    this.widget = widget;
    this.filter = filter;
    this.cutoff = cutoff;
    this.correction = correction;
  }
  
}
