package t.viewer.client.components.search;

import java.util.*;

import javax.annotation.Nullable;

import t.common.shared.sample.search.AndMatch;
import t.common.shared.sample.search.MatchCondition;
import t.model.sample.Attribute;
import t.viewer.client.Utils;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * And-conditions are stacked vertically as rows.
 */
public class AndEditor extends MatchEditor {

  private List<OrEditor> orEditors = new ArrayList<OrEditor>();
  
  VerticalPanel panel = Utils.mkVerticalPanel(true);
  
  public AndEditor(@Nullable MatchEditor parent, Collection<Attribute> parameters) {
    super(parent, parameters);
    initWidget(panel);
    OrEditor o = newOr();
    orEditors.add(o);  
    panel.add(o);    
    panel.addStyleName("samplesearch-andpanel");
  }
  
  public @Nullable MatchCondition getCondition() {
    List<MatchCondition> or = new ArrayList<MatchCondition>();
    for (OrEditor oc: orEditors) {
      if (oc.getCondition() != null) {
        or.add(oc.getCondition());
      }
    }
    if (or.size() > 1) {
      return new AndMatch(or);
    } else if (or.size() == 1) {
      return or.get(0);
    } else {
      return null;
    }
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
