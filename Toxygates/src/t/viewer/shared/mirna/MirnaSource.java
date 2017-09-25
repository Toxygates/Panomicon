package t.viewer.shared.mirna;

import java.io.Serializable;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class MirnaSource implements Serializable {
  //GWT constructor
  MirnaSource() {}
  
  private String title, id;
  private boolean hasScores, empirical;
  private Double limit;
  private int size;
  
  /**
   * Construct a new MiRNA source (mRNA-miRNA associations) information object.
   * 
   * @param id An identifying string. 
   * @param title The title of the source
   * @param empirical Whether the source is empirical
   * @param hasScoresWhether the associations have numerical scores
   * @param limit The cutoff limit, if any (if there are numerical scores)
   */
  public MirnaSource(String id, String title, boolean empirical, 
      boolean hasScores, @Nullable Double limit,
      int size) {
    this.id = id;
    this.title = title;
    this.empirical = empirical;
    this.hasScores = hasScores;
    this.limit = limit;
    this.size = size;
  }
  
  public String title() { return title; }
  
  public boolean hasScores() { return hasScores; }
  
  public boolean empirical() { return empirical; }
  
  public @Nullable Double limit() { return limit; }
  
  public void setLimit(double limit) { this.limit = limit; }
  
  public String id() { return id; }
  
  public int size() { return size; }
  
  public boolean equals(Object other) {
    if (other instanceof MirnaSource) {
      return id.equals(((MirnaSource) other).id());
    } else {
      return false;
    }
  }
  
  public int hashCode() {
    return id.hashCode();
  }
}
