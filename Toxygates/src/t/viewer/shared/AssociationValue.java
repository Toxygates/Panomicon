package t.viewer.shared;

import java.io.Serializable;

import javax.annotation.Nullable;

import t.common.shared.Pair;

@SuppressWarnings("serial")
public class AssociationValue implements Serializable {
  private String title;
  private String formalIdentifier;
  private @Nullable String tooltip;
  
  //GWT constructor
  AssociationValue() {}
  
  public AssociationValue(String title, String formalIdentifier, @Nullable String tooltip) {
    this.title = title;
    this.formalIdentifier = formalIdentifier;
    this.tooltip = tooltip;
  }
  
  public AssociationValue(Pair<String, String> value) {
    this(value.first(), value.second(), null);
  }
  
  public String title() {
    return title;
  }
  
  public String formalIdentifier() {
    return formalIdentifier;    
  }
  
  @Override
  public int hashCode() {
    return title.hashCode() * 41 + formalIdentifier.hashCode(); 
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof AssociationValue) {
      return formalIdentifier.equals(((AssociationValue) other).formalIdentifier());
    } else {
      return false;
    }
  }
  
  public String tooltip() {
    if (tooltip == null) {
      return formalIdentifier +": " + title; 
    } else {
      return tooltip;
    }
  }  
}
