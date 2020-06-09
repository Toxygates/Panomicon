/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.intermine;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import t.common.client.components.ItemSelector;
import t.viewer.client.screen.Screen;
import t.viewer.shared.intermine.*;

public class InterMineEnrichDialog extends InterMineSyncDialog {

  public InterMineEnrichDialog(Screen parent, String action,
      @Nullable IntermineInstance preferredInstance) {
    super(parent, action, false, false, preferredInstance, null);
  }

  @Override
  protected void userProceed(IntermineInstance instance, String user, String pass, boolean replace) {

  }

  private void addWithLabel(Grid g, int row, String l, Widget w) {
    g.setWidget(row, 0, new Label(l));
    g.setWidget(row, 1, w);    
  }
  
  VerticalPanel vp = new VerticalPanel();
  
  SimplePanel widgetSelHolder = new SimplePanel();
  ItemSelector<EnrichmentWidget> widgetSel = widgetSelector(new EnrichmentWidget[] {});
    
  
  ItemSelector<Correction> corr = new ItemSelector<Correction>() {
    @Override
    protected Correction[] values() {
      return Correction.values();
    }
  };
  
  TextBox pValueCutoff = new TextBox();
  ListBox filter = new ListBox();
  
  @Override
  protected Widget content() {
    Widget r = super.content();
    IntermineInstance inst = selector.value();
    if (inst != null) {
      instanceChanged(selector.value());
    }
    return r;
  }
  
  @Override
  protected Widget customUI() {
    widgetSelHolder.add(widgetSel);
    Grid g = new Grid(4, 2);
    vp.add(g);
    addWithLabel(g, 0, "Enrichment: ", widgetSelHolder);      
    addWithLabel(g, 1, "Filter: ", filter);
    pValueCutoff.setValue("0.05");
    addWithLabel(g, 2, "p-value cutoff: ", pValueCutoff);
    addWithLabel(g, 3, "Correction: ", corr);
    
    setFilterItems(EnrichmentWidget.values()[0].filterValues());
    
    return vp;
  }
  
  protected ItemSelector<EnrichmentWidget> widgetSelector(final EnrichmentWidget[] values) {
    return new ItemSelector<EnrichmentWidget>() {
      @Override
      protected EnrichmentWidget[] values() {
        return values;
      }      
      
      @Override
      protected void onValueChange(EnrichmentWidget selected) {
        setFilterItems(selected.filterValues());
      }
    };
  }
  
  @Override
  protected void instanceChanged(IntermineInstance instance) {
    super.instanceChanged(instance);
    setFilterItems(new String[]{});
    final EnrichmentWidget[] widgets = EnrichmentWidget.widgetsFor(instance);
    widgetSel = widgetSelector(widgets);
    if (widgets.length > 0) {
      widgetSel.setSelected(widgets[0]);
      setFilterItems(widgets[0].filterValues());
    }
    widgetSelHolder.clear();
    widgetSelHolder.add(widgetSel);
  }

  private void setFilterItems(String[] items) {
    filter.clear();
    for (String i: items) {
      filter.addItem(i);
    }
  }

  public double getCutoff() { return Double.parseDouble(pValueCutoff.getValue()); }
  
  public EnrichmentWidget getWidget() { return widgetSel.value(); }
  
  public Correction getCorrection() { return corr.value(); }
  
  public String getFilter() { return filter.getSelectedItemText(); }
  
  public EnrichmentParams getParams() {
    return new EnrichmentParams(getWidget(), getFilter(), getCutoff(), getCorrection());
  }
}
