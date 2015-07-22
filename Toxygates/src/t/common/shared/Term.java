package t.common.shared;

public class Term {
  private String term;
  private AType association;

  public Term(String term, AType association) {
    this.term = term;
    this.association = association;
  }

  public String getTermString() {
    return term;
  }

  public AType getAssociation() {
    return association;
  }
}
