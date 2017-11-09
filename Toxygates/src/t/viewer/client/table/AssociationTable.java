/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.table;

import java.util.*;

import javax.annotation.Nullable;

import otgviewer.client.components.Screen;
import t.common.client.components.StringArrayTable;
import t.common.shared.*;
import t.viewer.client.rpc.ProbeServiceAsync;
import t.viewer.shared.Association;
import t.viewer.shared.AssociationValue;
import t.viewer.shared.table.SortKey;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A RichTable that can display association columns.
 * 
 * @author johan
 *
 */
abstract public class AssociationTable<T> extends RichTable<T> {
  protected final ProbeServiceAsync probeService;
  protected Map<AType, Association> associations = new HashMap<AType, Association>();
  protected Map<AType, AssociationColumn> assocColumns;
  
  private boolean waitingForAssociations = true;

  public AssociationTable(Screen screen, TableStyle style) {
    super(screen, style);
    probeService = screen.manager().probeService();
  }

  protected List<HideableColumn<T, ?>> initHideableColumns(DataSchema schema) {
    SafeHtmlCell shc = new SafeHtmlCell();
    List<HideableColumn<T, ?>> r = new ArrayList<HideableColumn<T, ?>>();
    assocColumns = new HashMap<AType, AssociationColumn>();
    for (AType at : schema.associations()) {
      // TODO fill in matrixColumn for sortable associations
      AssociationColumn ac = new AssociationColumn(shc, at);
      r.add(ac);
      assocColumns.put(at, ac);
    }
    return r;
  }
  
  private AType[] visibleAssociations() {
    List<AType> r = new ArrayList<AType>();
    for (HideableColumn<T, ?> ac : hideableColumns) {
      if (ac instanceof AssociationTable.AssociationColumn) {
        if (ac.visible()) {
          r.add(((AssociationTable<?>.AssociationColumn) ac).assoc);
        }       
      }
    }
    return r.toArray(new AType[0]);
  }

  public void getAssociations() {
    waitingForAssociations = true;
    AType[] vas = visibleAssociations();
    if (vas.length > 0) {
      AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
        public void onFailure(Throwable caught) {
          Window.alert("Unable to get associations: " + caught.getMessage());
        }

        public void onSuccess(Association[] result) {
          associations.clear();
          waitingForAssociations = false;
          for (Association a : result) {
            associations.put(a.type(), a);
          };
          associationsUpdated();
          grid.redraw();
        }
      };

      logger.info("Get associations for " + chosenSampleClass.toString());
      probeService.associations(chosenSampleClass, vas, displayedAtomicProbes(), assocCallback);
    } else {
      logger.info("No associations to fetch");
    }
  }
  
  /**
   * Called when associations have been updated.
   */
  protected void associationsUpdated() {}

  /**
   * Get the atomic probes currently being displayed (keys for associations)
   * 
   * @return
   */
  abstract protected String[] displayedAtomicProbes();

  abstract protected String probeForRow(T row);

  abstract protected String[] atomicProbesForRow(T row);

  abstract protected String[] geneIdsForRow(T row);

  public static abstract class LinkingColumn<T> extends HTMLHideableColumn<T> {
    public LinkingColumn(SafeHtmlCell c, String name, boolean initState, String width) {
      super(c, name, initState, width);
    }

    public LinkingColumn(SafeHtmlCell c, String name, StandardColumns col, TableStyle style) {
      this(c, name, style.initVisibility(col), style.initWidth(col));
    }

    // TODO might move this method down or parameterise AssociationValue,
    // use an interface etc
    protected List<String> makeLinks(Collection<AssociationValue> values) {
      List<String> r = new ArrayList<String>();
      for (AssociationValue v : values) {
        String l = formLink(v.formalIdentifier());
        if (l != null) {
          r.add("<div class=\"associationValue\" title=\"" + v.tooltip()
              + "\"><a target=\"_TGassoc\" href=\"" + l + "\">" + v.title() + "</a></div>");
        } else {
          r.add("<div class=\"associationValue\" title=\"" + v.tooltip() + "\">" + v.title()
              + "</div>"); // no link
        }
      }
      return r;
    }

    @Override
    protected String getHtml(T er) {
      return SharedUtils.mkString(makeLinks(getLinkableValues(er)), "");
    }

    protected Collection<AssociationValue> getLinkableValues(T er) {
      return new ArrayList<AssociationValue>();
    }

    protected abstract String formLink(String value);

  }

  public class AssociationColumn extends LinkingColumn<T> implements MatrixSortable {
    private AType assoc;

    /**
     * @param tc
     * @param association
     * @param matrixIndex Underlying data index for a corresponding hidden sorting column. Only
     *        meaningful if this association is sortable.
     */
    public AssociationColumn(SafeHtmlCell tc, AType association) {
      super(tc, association.title(), false, "15em");
      this.assoc = association;
      this._columnInfo = new ColumnInfo(_name, _width, association.canSort(), true, true, false);
    }

    public AType getAssociation() {
      return assoc;
    }

    public SortKey sortKey() {
      return new SortKey.Association(assoc);
    }

    @Override
    void setVisibility(boolean v) {
      super.setVisibility(v);
    }

    protected String formLink(String value) {
      return assoc.formLink(value);
    }

    protected Collection<AssociationValue> getLinkableValues(T expressionRow) {
      Association a = associations.get(assoc);
      Set<AssociationValue> all = new HashSet<AssociationValue>();
      if (a == null) {
        return all;
      }
      for (String at : atomicProbesForRow(expressionRow)) {
        if (a.data().containsKey(at)) {
          all.addAll(a.data().get(at));
        }
      }
      for (String gi : geneIdsForRow(expressionRow)) {
        if (a.data().containsKey(gi)) {
          all.addAll(a.data().get(gi));
        }
      }
      return all;
    }

    @Override
    protected String getHtml(T er) {
      if (waitingForAssociations) {
        return ("(Waiting for data...)");
      } else if (associations.containsKey(assoc)) {
        return super.getHtml(er);
      }
      return ("(Data unavailable)");
    }
  }
  
  /**
   * Display a summary of a column.
   */
  public void displayColumnSummary(AssociationColumn col) {
    AssociationSummary<T> summary = associationSummary(col);
    StringArrayTable.displayDialog(summary.getTable(), col.getAssociation().title() + " summary",
      500, 500);
  }

  @Nullable 
  public AssociationSummary<T> associationSummary(AType atype) {
    AssociationColumn col = assocColumns.get(atype);
    if (col == null) {
      return null;
    }
    return associationSummary(col);
  }
  
  AssociationSummary<T> associationSummary(AssociationColumn col) {
    return new AssociationSummary<T>(col, grid.getDisplayedItems());
  }
  
}
