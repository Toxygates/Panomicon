package otgviewer.client.dialog;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.shared.targetmine.Correction;
import otgviewer.shared.targetmine.EnrichmentParams;
import otgviewer.shared.targetmine.EnrichmentWidget;
import t.common.client.components.EnumSelector;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class TargetMineEnrichDialog extends TargetMineSyncDialog {

  public TargetMineEnrichDialog(DataListenerWidget parent, String url, String action) {
    super(parent, url, action, false);
  }

  @Override
  protected void userProceed(String user, String pass, boolean replace) {

  }

  private void addWithLabel(Grid g, int row, String l, Widget w) {
    g.setWidget(row, 0, new Label(l));
    g.setWidget(row, 1, w);    
  }
  
  VerticalPanel vp = new VerticalPanel();
  
  EnumSelector<EnrichmentWidget> widget = new EnumSelector<EnrichmentWidget>() {
    @Override
    protected EnrichmentWidget[] values() {
      return EnrichmentWidget.values();
    }      
    
    @Override
    protected void onValueChange(EnrichmentWidget selected) {
      setFilterItems(selected.filterValues());
    }
  };
  
  EnumSelector<Correction> corr = new EnumSelector<Correction>() {
    @Override
    protected Correction[] values() {
      return Correction.values();
    }
  };
  
  TextBox pValueCutoff = new TextBox();
  ListBox filter = new ListBox();
  
  @Override
  protected Widget customUI() {
    Grid g = new Grid(4, 2);
    vp.add(g);
    addWithLabel(g, 0, "Enrichment: ", widget);      
    addWithLabel(g, 1, "Filter: ", filter);
    pValueCutoff.setValue("0.05");
    addWithLabel(g, 2, "p-value cutoff: ", pValueCutoff);
    addWithLabel(g, 3, "Correction: ", corr);
    
    setFilterItems(EnrichmentWidget.values()[0].filterValues());
    
    return vp;
  }
  
  private void setFilterItems(String[] items) {
    filter.clear();
    for (String i: items) {
      filter.addItem(i);
    }
  }

  //TODO check format
  public double getCutoff() { return Double.parseDouble(pValueCutoff.getValue()); }
  
  public EnrichmentWidget getWidget() { return widget.value(); }
  
  public Correction getCorrection() { return corr.value(); }
  
  public String getFilter() { return filter.getSelectedItemText(); }
  
  public EnrichmentParams getParams() {
    return new EnrichmentParams(getWidget(), getFilter(), getCutoff(), getCorrection());
  }
}
