package t.viewer.shared.mirna;

import java.io.Serializable;

import javax.annotation.Nullable;

import t.common.shared.Packable;

@SuppressWarnings("serial")
public class MirnaSource implements Serializable, Packable {
  //GWT constructor
  MirnaSource() {}
  
  private String title, id;
  private boolean hasScores, experimental;
  private Double limit;
  private int size;
  
  /**
   * Construct a new MiRNA source (mRNA-miRNA associations) information object.
   * 
   * @param id An identifying string. 
   * @param title The title of the source
   * @param experimental Whether the source is experimentally validated
   * @param hasScoresWhether the associations have numerical scores
   * @param limit The cutoff limit, if any (if there are numerical scores)
   */
  public MirnaSource(String id, String title, boolean experimental, 
      boolean hasScores, @Nullable Double limit,
      int size) {
    this.id = id;
    this.title = title;
    this.experimental = experimental;
    this.hasScores = hasScores;
    this.limit = limit;
    this.size = size;
  }
  
  public String title() { return title; }
  
  public boolean hasScores() { return hasScores; }
  
  public boolean experimental() { return experimental; }
  
  public @Nullable Double limit() { return limit; }
  
  public void setLimit(Double limit) { this.limit = limit; }
  
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
  
  public String pack() {
    return id + "^^^" + limit;
  }
  
  public static MirnaSource unpack(String state) {
    String[] spl = state.split("\\^\\^\\^");
    Double limit;
    try {
      if (spl.length == 2) {
        limit = Double.parseDouble(spl[1]);
      } else {
        return null;
      }
    } catch (NumberFormatException e) {
      limit = null;
    }
    if (spl.length == 2) {
      return new MirnaSource(spl[0], "", false, false, limit, 0);
    } else {
      return null;
    }    
  }
}
