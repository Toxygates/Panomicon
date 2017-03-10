package t.viewer.client.components.search;

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.Composite;

abstract public class MatchEditor extends Composite {

  protected Collection<String> parameters;
  
  protected @Nullable MatchEditor parent;
  
  private boolean expanded = false;
  
  public MatchEditor(@Nullable MatchEditor parent, Collection<String> parameters) {
    super();
    this.parameters = parameters;
    this.parent = parent;
  }
  
  void signalEdit() {
    if (parent != null) {
      parent.childEdited();
    }
  }
  
  /**
   * Invoked by child editors when they are edited.
   */
  void childEdited() {
    if (!expanded) {
      expanded = true;
      expand();
      signalEdit();
    }
  }
  
  /**
   * Expand this editor by adding more children.
   */
  protected void expand() { }

}
