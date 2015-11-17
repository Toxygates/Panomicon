package otgviewer.shared.targetmine;

import java.io.Serializable;

@SuppressWarnings("serial")
public class EnrichmentParams implements Serializable {

  public EnrichmentWidget widget;
  public double cutoff;
  public Correction correction;
  
  //GWT constructor
  public EnrichmentParams() {}
  
  //TODO add "filter" - however, this is different for different widgets.
  
  public EnrichmentParams(EnrichmentWidget widget, double cutoff, Correction correction) {
    this.widget = widget;
    this.cutoff = cutoff;
    this.correction = correction;
  }
  
}
