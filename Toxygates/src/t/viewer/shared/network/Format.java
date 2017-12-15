package t.viewer.shared.network;

/**
 * Network file formats
 */
public enum Format {
  DOT("dot"),
  Custom("txt");
  
  Format(String suffix) {
    this.suffix = suffix;
  }
  private String suffix;
  
  public String suffix() { return suffix; }
}
