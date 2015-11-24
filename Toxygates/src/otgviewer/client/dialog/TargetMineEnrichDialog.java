package otgviewer.client.dialog;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.shared.targetmine.Correction;
import otgviewer.shared.targetmine.EnrichmentParams;
import otgviewer.shared.targetmine.EnrichmentWidget;
import t.common.client.components.EnumSelector;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class TargetMineEnrichDialog extends TargetMineSyncDialog {

  public TargetMineEnrichDialog(DataListenerWidget parent, String url, String action) {
    super(parent, url, action);
  }

  @Override
  protected void userProceed(String user, String pass, boolean replace) {

  }

  private void addWithLabel(String l, Widget w) {
    HorizontalPanel hp = new HorizontalPanel();
    hp.add(new Label(l));
    hp.add(w);
    vp.add(hp);
  }
  
  VerticalPanel vp = new VerticalPanel();
  
  EnumSelector<EnrichmentWidget> widget = new EnumSelector<EnrichmentWidget>() {
    @Override
    protected EnrichmentWidget[] values() {
      return EnrichmentWidget.values();
    }      
  };
  
  EnumSelector<Correction> corr = new EnumSelector<Correction>() {
    @Override
    protected Correction[] values() {
      return Correction.values();
    }
  };
  
  TextBox pValueCutoff = new TextBox();
  
  @Override
  protected Widget customUI() { 
    addWithLabel("Enrichment: ", widget);        
    pValueCutoff.setValue("0.05");
    addWithLabel("p-value cutoff: ", pValueCutoff);
    addWithLabel("Correction: ", corr);
    
    return vp;
  }

  //TODO check format
  public double getCutoff() { return Double.parseDouble(pValueCutoff.getValue()); }
  
  public EnrichmentWidget getWidget() { return widget.value(); }
  
  public Correction getCorrection() { return corr.value(); }
  
  public EnrichmentParams getParams() {
    return new EnrichmentParams(getWidget(), getCutoff(), getCorrection());
  }
}
