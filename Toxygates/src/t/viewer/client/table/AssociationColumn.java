package t.viewer.client.table;

import java.util.*;

import com.google.gwt.cell.client.SafeHtmlCell;

import t.common.shared.AType;
import t.viewer.shared.*;

public class AssociationColumn<T> extends LinkingColumn<T> implements MatrixSortable {
  private Delegate<T> delegate;
  AType assoc;

  public interface Delegate<T> {
    boolean refreshEnabled();
    void getAssociations(AType[] associationsToFetch);
    String[] atomicProbesForRow(T row);
    String[] geneIdsForRow(T row);
    Map<AType, Association> associations();
  }

  static String columnWidth(AType assoc) {
    switch (assoc) {
      case MiRNA:
        return "18em";
      default:
        return "15em";
    }
  }

  public AssociationColumn(SafeHtmlCell tc, AType association, Delegate<T> delegate) {
    super(tc, association.title(), false, columnWidth(association), null);
    this.delegate = delegate;
    this.assoc = association;
    this._columnInfo = new ColumnInfo(_name, _width, association.canSort(), true, true, false);
  }

  public AType getAssociation() {
    return assoc;
  }

  @Override
  public SortKey sortKey() {
    return new SortKey.Association(assoc);
  }

  @Override
  public void setVisibility(boolean v) {
    super.setVisibility(v);
    if (v && delegate.refreshEnabled()) {
      delegate.getAssociations(new AType[]{getAssociation()});
    }
  }

  @Override
  protected String formLink(String value) {
    return assoc.formLink(value);
  }

  @Override
  protected Collection<AssociationValue> getLinkableValues(T expressionRow) {
    Association a = delegate.associations().get(assoc);
    Set<AssociationValue> all = new HashSet<AssociationValue>();
    if (a == null) {
      return all;
    }

    for (String at : delegate.atomicProbesForRow(expressionRow)) {
      Set<AssociationValue> result = a.data().get(at);
      if (result != null) {
        all.addAll(result);
      }
    }
    for (String gi : delegate.geneIdsForRow(expressionRow)) {
      Set<AssociationValue> result = a.data().get(gi);
      if (result != null) {
        all.addAll(result);
      }
    }
    return all;
  }

  @Override
  protected String getHtml(T er) {
    if (delegate.associations().containsKey(assoc)) {
      return super.getHtml(er);
    } else {
    	return ("(Waiting for data...)");
    } 
  }
}