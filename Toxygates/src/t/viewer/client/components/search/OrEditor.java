package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.sample.search.AtomicMatch;
import t.common.shared.sample.search.OrMatch;
import t.viewer.client.Utils;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class OrEditor extends MatchEditor {

  private List<AtomicEditor> atomicEditors = new ArrayList<AtomicEditor>();
  
  private HorizontalPanel panel = Utils.mkHorizontalPanel(true);
  
  public OrEditor(@Nullable MatchEditor parent, Collection<String> parameters) {
    super(parent, parameters);
    initWidget(panel);    
    AtomicEditor a = newAtomic();
    atomicEditors.add(a);
    panel.add(a);
    panel.addStyleName("samplesearch-orpanel");
  }

  public OrMatch getCondition() {
    List<AtomicMatch> atomics = new ArrayList<AtomicMatch>();
    for (AtomicEditor ac: atomicEditors) {
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
    super.expand();
    AtomicEditor a = newAtomic();
    atomicEditors.add(a);    
    Label l = mkLabel("OR");
    panel.add(l);
    panel.add(a);    
  }
}
