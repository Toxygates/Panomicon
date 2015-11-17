package otgviewer.shared.targetmine;

public enum Correction {
  HolmBonferroni("Holm-Bonferroni"), 
  BenjaminiHochberg("Benjamini Hochberg"), 
  Bonferroni("Bonferroni"), 
  None("None");
  
  String repr;
  
  Correction(String repr) {
    this.repr = repr;
  }
  
  public String getKey() {
    return repr;
  }
}
