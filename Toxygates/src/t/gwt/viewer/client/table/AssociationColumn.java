/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.viewer.client.table;

import com.google.gwt.cell.client.SafeHtmlCell;
import t.shared.common.AType;
import t.shared.viewer.Association;
import t.shared.viewer.AssociationValue;
import t.shared.viewer.SortKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssociationColumn<T> extends LinkingColumn<T> implements MatrixSortable {
  private Delegate<T> delegate;
  AType assoc;

  public interface Delegate<T> {
    boolean refreshEnabled();
    void enableAssociation(AType association);
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
      delegate.enableAssociation(getAssociation());
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