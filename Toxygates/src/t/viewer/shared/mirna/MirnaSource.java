package t.viewer.shared.mirna;

import java.io.Serializable;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class MirnaSource implements Serializable {
  //GWT constructor
  MirnaSource() {}
  
  private String title;
  private boolean hasScores, empirical;
  private Double suggestedLimit;
  
  /**
   * Construct a new MiRNA source (mRNA-miRNA associations) information object.
   * 
   * @param title The title of the source
   * @param empirical Whether the source is empirical
   * @param hasScoresWhether the associations have numerical scores
   * @param suggestedLimit The suggested limit, if any (if there are numerical scores)
   */
  public MirnaSource(String title, boolean empirical, 
      boolean hasScores, @Nullable Double suggestedLimit,
      int size) {
    this.title = title;
    this.empirical = empirical;
    this.hasScores = hasScores;
    this.suggestedLimit = suggestedLimit;
  }
  
  public String title() { return title; }
  
  public boolean hasScores() { return hasScores; }
  
  public boolean empirical() { return empirical; }
  
  public @Nullable Double suggestedLimit() { return suggestedLimit; }
}
