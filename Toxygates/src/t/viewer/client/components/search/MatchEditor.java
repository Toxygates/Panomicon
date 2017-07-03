package t.viewer.client.components.search;

import java.util.Collection;

import javax.annotation.Nullable;

import t.common.shared.sample.BioParamValue;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

abstract public class MatchEditor extends Composite {

  protected Collection<BioParamValue> parameters;
  
  protected @Nullable MatchEditor parent;
  
  private boolean signalled = false;
  
  public MatchEditor(@Nullable MatchEditor parent, Collection<BioParamValue> parameters) {
    super();
    this.parameters = parameters;
    this.parent = parent;
  }
  
  void signalEdit() {
    if (parent != null) {
      if (!signalled) {
        signalled = true;
        parent.childFirstEdit();        
      }
      parent.childEdit();
    }
  }
  
  /**
   * Invoked by child editors when they are edited for the first time.
   */
  void childFirstEdit() {
    expand();
    signalEdit();
  }
  
  /**
   * Invoked by child editors when they are edited.
   */
  void childEdit() {
    signalEdit();
  }
  
  /**
   * Expand this editor by adding more children.
   */
  protected void expand() { }
  
  protected Label mkLabel(String string) {
    Label l = new Label(string);
    l.addStyleName("samplesearch-label");
    return l;
  }

}
