package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.sample.search.AndMatch;
import t.common.shared.sample.search.OrMatch;

import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * And-conditions are stacked vertically as rows.
 */
public class AndEditor extends MatchEditor {

  private List<OrEditor> orConds = new ArrayList<OrEditor>();
  
  VerticalPanel panel = new VerticalPanel();
  
  public AndEditor(@Nullable MatchEditor parent, Collection<String> parameters) {
    super(parent, parameters);
    initWidget(panel);
  }
  
  public AndMatch getCondition() {
    List<OrMatch> or = new ArrayList<OrMatch>();
    for (OrEditor oc: orConds) {
      or.add(oc.getCondition());
    }
    return new AndMatch(or);
  }
  
  OrEditor newOr() {
    return new OrEditor(this, parameters);
  }

  @Override
  protected void expand() {
    
  }
}
