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

package t.viewer.client.table;

import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import t.viewer.client.screen.data.FilterEditor;
import t.common.shared.GroupUtils;
import t.common.shared.SharedUtils;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.ClientGroup;
import t.viewer.client.Utils;
import t.viewer.client.screen.Screen;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.ColumnFilter;
import t.viewer.shared.ManagedMatrixInfo;
import t.viewer.shared.SortKey;
import t.viewer.shared.Synthetic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for ExpressionTable, encapsulating operations involving a
 * ManagedMatrixInfo, often involving communication with the server through a
 * MatrixServiceAsync.
 */
public class ETMatrixManager {

  private Screen screen;
  private final MatrixServiceAsync matrixService;
  private String matrixId;
  private ManagedMatrixInfo matrixInfo = null;
  private KCAsyncProvider asyncProvider = new KCAsyncProvider();
  private DialogBox filterDialog = null;
  private final Logger logger = SharedUtils.getLogger("matrixManager");
  private Delegate delegate;
  private Loader loader;

  /**
   * Names of the probes currently displayed
   */
  private String[] displayedAtomicProbes = new String[0];
  /**
   * Names of the (potentially merged) probes being displayed
   */
  private String[] displayedProbes = new String[0];

  private List<Group> lastColumnGroups = null;
  private List<ColumnFilter> lastColumnFilters = new ArrayList<ColumnFilter>();

  public interface Delegate {
    void setupColumns();
    void onGettingExpressionFailed();
    void setEnabled(boolean enabled);
    List<ClientGroup> chosenColumns();
    void getExpressions();
    SortOrder computeSortParams();
    void onApplyColumnFilter();
    void onGetRows();
    void onSetRowCount(int visibleRows);
  }

  interface Loader {
    /**
     * Perform an initial matrix load. The method should load data and then call
     * setInitialMatrix.
     * @param initPageSize initial page size. Provided as a hint to the loader to optimize
     * certain operations.
     */
    void loadInitialMatrix(ValueType valueType, int initPageSize, List<ColumnFilter> initFilters);
  }

  public ETMatrixManager(Screen screen, TableFlags flags, Delegate delegate, Loader loader,
                         DataGrid<ExpressionRow> grid) {
    this.screen = screen;
    this.loader = loader;
    this.delegate = delegate;
    this.matrixService = screen.manager().matrixService();
    this.matrixId = flags.matrixId;
    asyncProvider.addDataDisplay(grid);
  }

  public @Nullable ManagedMatrixInfo info() {
    return matrixInfo;
  }

  public String id() {
    return matrixId;
  }

  public List<ColumnFilter> columnFilters() {
    return matrixInfo.columnFilters();
  }

  public List<ColumnFilter> lastColumnFilters() {
    return lastColumnFilters;
  }

  public String[] displayedProbes() {
    return displayedProbes;
  }

  public String[] displayedAtomicProbes() {
    return displayedAtomicProbes;
  }

  protected boolean hasPValueColumns() {
    for (int i = 0; i < matrixInfo.numDataColumns(); ++i) {
      if (matrixInfo.isPValueColumn(i)) {
        return true;
      }
    }
    return false;
  }

  protected boolean isMergeMode() {
    if (displayedProbes == null || displayedAtomicProbes == null) {
      return false;
    }
    return displayedProbes.length != displayedAtomicProbes.length;
  }

  public void logInfo(String msg) {
    logger.info("Matrix " + matrixId + ": " + msg);
  }

  public void log(Level level, String msg, Throwable throwable) {
    logger.log(level, "Matrix " + matrixId + ":" + msg, throwable);
  }

  /**
   * To be called when a new matrix is set (as opposed to partial refinement or
   * modification of a previously loaded matrix).
   */
  void setInitialMatrix(String[] requestedProbes, ManagedMatrixInfo matrix) {
    if (requestedProbes.length == 0 || matrix.numRows() > 0) {
      matrixInfo = matrix;
      
      if (lastColumnGroups == null ||
          lastColumnGroups.size() != matrix.columnGroups().size() ||
          !lastColumnGroups.containsAll(matrix.columnGroups())) {
        if (matrixInfo.isOrthologous()) {
          Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_VIEW_ORTHOLOGOUS_DATA);
        }
      }
      
      lastColumnGroups = matrix.columnGroups();

      matrixInfo = matrix;
      delegate.setupColumns();      
      setRows(matrix.numRows());

      logInfo("Data successfully loaded");
      logInfo(matrixInfo.getAtomicProbes(false).length + " atomic probes");
    } else {
      delegate.onGettingExpressionFailed();
    }
  }

  void removeTests() {
    matrixService.removeSyntheticColumns(matrixId, new PendingAsyncCallback<ManagedMatrixInfo>(
        screen.manager(), "There was an error removing the test columns.") {
      /*
       * Note that the number of rows can change as a result of this operation, due to filtering
       * being affected.
       */
      @Override
      public void handleSuccess(ManagedMatrixInfo result) {
        setInitialMatrix(new String[] {}, result);
      }
    });
  }

  void addTwoGroupSynthetic(final Synthetic.TwoGroupSynthetic synth, final String name,
      String selectedGroup1, String selectedGroup2) {
    final Group g1 = GroupUtils.findGroup(delegate.chosenColumns(), selectedGroup1).get().convertToGroup();
    final Group g2 = GroupUtils.findGroup(delegate.chosenColumns(), selectedGroup2).get().convertToGroup();
    synth.setGroups(g1, g2);

    matrixService.addSyntheticColumn(matrixId, synth,
        new PendingAsyncCallback<ManagedMatrixInfo>(screen.manager(), "Adding test column failed") {
          @Override
          public void handleSuccess(ManagedMatrixInfo r) {
            matrixInfo = r;
            setRows(r.numRows());
            delegate.setupColumns();
          }
        });
  }

  public void downloadCSV(boolean individualSamples) {
    matrixService.prepareCSVDownload(matrixId, individualSamples,
        new PendingAsyncCallback<String>(screen.manager(), "Unable to prepare the requested data for download.") {
          @Override
          public void handleSuccess(String url) {
            Utils.displayURL("Your download is ready.", "Download", url);
          }
        });
  }

  public void editColumnFilter(int column) {
    ColumnFilter filt = matrixInfo.columnFilter(column);
    FilterEditor fe = new FilterEditor(matrixInfo.columnName(column), column, filt) {
      @Override
      protected void onChange(ColumnFilter newVal) {
        delegate.onApplyColumnFilter();
        applyColumnFilter(editColumn, newVal);
      }
    };
    filterDialog = Utils.displayInPopup("Edit filter", fe, DialogPosition.Center);
  }

  protected void applyColumnFilter(final int column, final @Nullable ColumnFilter filter) {
    delegate.setEnabled(false);
    filterDialog.setVisible(false);
    matrixService.setColumnFilter(matrixId, column, filter, new AsyncCallback<ManagedMatrixInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("An error occurred when the column filter was changed.");
        delegate.setEnabled(true);
      }

      @Override
      public void onSuccess(ManagedMatrixInfo result) {
        if (result.numRows() == 0 && filter.active()) {
          Window.alert("No rows match the selected filter. The filter will be reset.");
          applyColumnFilter(column, filter.asInactive());
        } else {
          matrixInfo = result;
          setRows(matrixInfo.numRows());
          delegate.setupColumns();
        }
      }
    });
  }

  protected void clearColumnFilters(final int[] columns) {
    delegate.setEnabled(false);
    filterDialog.setVisible(false);
    matrixService.clearColumnFilters(matrixId, columns, new AsyncCallback<ManagedMatrixInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("An error occurred when column filters were cleared.");
        delegate.setEnabled(true);
      }

      @Override
      public void onSuccess(ManagedMatrixInfo result) {
        matrixInfo = result;
        setRows(matrixInfo.numRows());
        delegate.setupColumns();
      }
    });
  }

  /**
   * Filter data that has already been loaded
   */
  public void refilterData(final String[] chosenProbes) {
    asyncProvider.updateRowCount(0, false);
    delegate.setEnabled(false);
    logInfo("Refilter for " + chosenProbes.length + " probes");
    matrixService.selectProbes(matrixId, chosenProbes, new AsyncCallback<ManagedMatrixInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        log(Level.WARNING, "Exception in data update callback", caught);
        delegate.getExpressions(); // the user probably let the session expire
      }

      @Override
      public void onSuccess(ManagedMatrixInfo result) {
        if (chosenProbes.length == 0 || result.numRows() > 0) {
          matrixInfo = result;
          setRows(matrixInfo.numRows());
        } else {
          //This only happens if a non-empty probe set with no valid probes
          //was requested.
          delegate.onGettingExpressionFailed();
        }
      }
    });
  }

  public static class SortOrder {
    public SortKey key;
    public boolean asc;

    public SortOrder(SortKey key, boolean ascending) {
      this.key = key;
      this.asc = ascending;
    }
  }

  public void clear() {
    asyncProvider.updateRowCount(0, true);
  }

  public void setRows(int numRows) {
    if (matrixInfo != null) {
      lastColumnFilters = matrixInfo.columnFilters();
    }
    asyncProvider.updateRowCount(numRows, true);
    int initSize = NavigationTools.INIT_PAGE_SIZE;
    int displayRows = (numRows > initSize) ? initSize : numRows;
    delegate.onSetRowCount(displayRows);
    delegate.setEnabled(true);
  }

  public void getExpressions(boolean preserveFilters, ValueType chosenValueType) {
    delegate.setEnabled(false);
    List<ColumnFilter> initFilters = preserveFilters ? lastColumnFilters : new ArrayList<ColumnFilter>();
    asyncProvider.updateRowCount(0, false);
    loader.loadInitialMatrix(chosenValueType, NavigationTools.INIT_PAGE_SIZE, initFilters);
  }

  class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
    private Range range;
    AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {

      private String errMsg() {
        String appName = screen.appInfo().applicationName();
        return "Unable to obtain data. If you have not used " + appName + " in a while, try reloading the page.";
      }

      @Override
      public void onFailure(Throwable caught) {
        logger.log(Level.INFO, "KCAsyncProvider data fetch exception", caught);
        Window.alert(errMsg());
      }

      @Override
      public void onSuccess(List<ExpressionRow> result) {
        if (result.size() > 0) {
          updateRowData(range.getStart(), result);
          displayedAtomicProbes = result.stream().flatMap(r -> Arrays.stream(r.getAtomicProbes()))
              .toArray(String[]::new);
          displayedProbes = result.stream().map(r -> r.getProbe()).toArray(String[]::new);
          delegate.onGetRows();
        } else {
          setRows(0);
        }
      }
    };

    @Override
    protected void onRangeChanged(HasData<ExpressionRow> display) {
      range = display.getVisibleRange();
      SortOrder order = delegate.computeSortParams();

      if (matrixInfo == null) {
        logger.warning("Asked to fetch range: " + range.getStart() + ", " + range.getLength()
            + " but no matrix loaded");
        // We hit this case on startup. It's not currently clear why rangeChanged events are sent
        // even before a valid row count has been established.
        return;
      }

      if (range.getLength() > 0) {
        matrixService.matrixRows(matrixId, range.getStart(), range.getLength(), order.key,
            order.asc, rowCallback);
      }
    }
  }
}
