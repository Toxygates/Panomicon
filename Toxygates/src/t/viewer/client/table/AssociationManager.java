/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import otg.viewer.client.components.OTGScreen;
import t.common.client.components.StringArrayTable;
import t.common.shared.AType;
import t.common.shared.DataSchema;
import t.common.shared.sample.ExpressionRow;
import t.model.SampleClass;
import t.viewer.client.rpc.ProbeServiceAsync;
import t.viewer.shared.Association;

/**
 * Manages Associations for a RichTable
 */
public class AssociationManager<T extends ExpressionRow> implements AssociationColumn.Delegate<T> {
  protected final ProbeServiceAsync probeService;
  protected Map<AType, Association> associations = new HashMap<AType, Association>();
  protected Map<AType, AssociationColumn<T>> assocColumns;
  
  private boolean waitingForAssociations = true;
  private boolean refreshEnabled = true;

  private Logger logger;
  private TableDelegate<T> tableDelegate;
  private ViewDelegate<T> viewDelegate;
  private RichTable<T> table;

  public interface TableDelegate<T> {
    SampleClass chosenSampleClass();
    String[] displayedAtomicProbes();
    String[] atomicProbesForRow(T row);
    String[] geneIdsForRow(T row);
  }

  public interface ViewDelegate<T extends ExpressionRow> {
    void associationsUpdated(AssociationManager<T> associations, Association[] result);
    //    void beforeGetAssociations(AssociationManager<T> associations);
  }

  public AssociationManager(OTGScreen screen, RichTable<T> table, TableDelegate<T> tableDelegate,
      ViewDelegate<T> viewDelegate) {
    probeService = screen.manager().probeService();
    logger = screen.getLogger();
    this.table = table;
    this.tableDelegate = tableDelegate;
    this.viewDelegate = viewDelegate;
  }

  protected List<HideableColumn<T, ?>> createHideableColumns(DataSchema schema) {
    SafeHtmlCell shc = new SafeHtmlCell();
    List<HideableColumn<T, ?>> r = new ArrayList<HideableColumn<T, ?>>();
    assocColumns = new HashMap<AType, AssociationColumn<T>>();

    for (AType at : schema.associations()) {
      AssociationColumn<T> ac = new AssociationColumn<T>(shc, at, this);
      r.add(ac);
      assocColumns.put(at, ac);
    }
    return r;
  }

  protected AType[] visibleAssociations() {
    List<AType> r = new ArrayList<AType>();
    for (AssociationColumn<T> ac : assocColumns.values()) {
      if (ac.visible()) {
        r.add(((AssociationColumn<?>) ac).assoc);
      }
    }
    return r.toArray(new AType[0]);
  }
  
  public boolean isVisible(AType associationType) {
    return assocColumns.get(associationType).visible();
  }
  
  /**
   * Display or hide an association column by its AType.
   */
  public void setVisible(AType associationType, boolean newState) {
    AssociationColumn<T> aColumn = assocColumns.get(associationType);
    table.setVisible(aColumn, newState);
  }

  @Override
  public void getAssociations() {
    //    viewDelegate.beforeGetAssociations(this);
    waitingForAssociations = true;
    AType[] assocs = visibleAssociations();
    String[] dispAtomic = tableDelegate.displayedAtomicProbes();
    if (assocs.length > 0 && dispAtomic.length > 0) {
      AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Unable to get associations: " + caught.getMessage());
        }

        @Override
        public void onSuccess(Association[] result) {
          associations.clear();
          waitingForAssociations = false;
          for (Association a : result) {
            associations.put(a.type(), a);
          };
          viewDelegate.associationsUpdated(AssociationManager.this, result);
          table.redrawGrid();
        }
      };

      logger
          .info("Get associations for " + dispAtomic.length + " probes in "
              + tableDelegate.chosenSampleClass().toString());
      probeService.associations(tableDelegate.chosenSampleClass(), assocs, dispAtomic, assocCallback);
    } else {
      logger.info("No associations to fetch");
    }
  }
  
  /**
   * If this flag is true (which is the default), 
   * associations are auto-refreshed when they are initially displayed.
   */
  public void setAssociationAutoRefresh(boolean autoRefresh) {
    this.refreshEnabled = autoRefresh;
  }
  
  /**
   * Display a summary of a column.
   */
  public void displayColumnSummary(AssociationColumn<T> col) {
    AssociationSummary<T> summary = associationSummary(col);
    StringArrayTable.displayDialog(summary.getTable(), col.getAssociation().title() + " summary",
      500, 500);
  }

  @Nullable 
  public AssociationSummary<T> associationSummary(AType atype) {
    AssociationColumn<T> col = assocColumns.get(atype);
    if (col == null) {
      logger.warning("No association summary available for atype " + atype);
      return null;
    }
    return associationSummary(col);
  }
  
  AssociationSummary<T> associationSummary(AssociationColumn<T> col) {
    return new AssociationSummary<T>(col, table.visibleItems());
  }

  // AssociationColumn.Delegate methods
  @Override
  public boolean refreshEnabled() {
    return refreshEnabled;
  }

  @Override
  public boolean waitingForAssociations() {
    return waitingForAssociations;
  }

  @Override
  public Map<AType, Association> associations() {
    return associations;
  }

  @Override
  public String[] atomicProbesForRow(T row) {
    return tableDelegate.atomicProbesForRow(row);
  }

  @Override
  public String[] geneIdsForRow(T row) {
    return tableDelegate.geneIdsForRow(row);
  }
}
