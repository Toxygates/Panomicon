package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.sample.search.AtomicMatch;
import t.common.shared.sample.search.OrMatch;

import com.google.gwt.user.client.ui.HorizontalPanel;

public class OrEditor extends MatchEditor {

  private List<AtomicEditor> atomicConds = new ArrayList<AtomicEditor>();
  
  private HorizontalPanel panel = new HorizontalPanel();
  
  public OrEditor(@Nullable MatchEditor parent, Collection<String> parameters) {
    super(parent, parameters);
    initWidget(panel);    
  }

  public OrMatch getCondition() {
    List<AtomicMatch> atomics = new ArrayList<AtomicMatch>();
    for (AtomicEditor ac: atomicConds) {
      AtomicMatch match = ac.getCondition();
      if (match != null) {
        atomics.add(match);
      }
    }
    return new OrMatch(atomics);
  }
  
  AtomicEditor newAtomic() {
    return new AtomicEditor(this, parameters);
  }
  
  @Override
  protected void expand() {
    
  }
}
