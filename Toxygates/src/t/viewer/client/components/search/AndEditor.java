package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.sample.search.AndMatch;
import t.common.shared.sample.search.OrMatch;
import t.viewer.client.Utils;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * And-conditions are stacked vertically as rows.
 */
public class AndEditor extends MatchEditor {

  private List<OrEditor> orEditors = new ArrayList<OrEditor>();
  
  VerticalPanel panel = Utils.mkVerticalPanel(true);
  
  public AndEditor(@Nullable MatchEditor parent, Collection<String> parameters) {
    super(parent, parameters);
    initWidget(panel);
    OrEditor o = newOr();
    orEditors.add(o);  
    panel.add(o);    
    panel.addStyleName("samplesearch-andpanel");
  }
  
  public AndMatch getCondition() {
    List<OrMatch> or = new ArrayList<OrMatch>();
    for (OrEditor oc: orEditors) {
      or.add(oc.getCondition());
    }
    return new AndMatch(or);
  }
  
  OrEditor newOr() {
    return new OrEditor(this, parameters);
  }

  @Override
  protected void expand() {
    super.expand();
    OrEditor o = newOr();
    orEditors.add(o);    
    Label l = mkLabel("AND");    
    panel.add(l);
    panel.add(o);    
  }
}
