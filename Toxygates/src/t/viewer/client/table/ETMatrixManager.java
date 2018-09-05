package t.viewer.client.table;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.view.client.*;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.FilterEditor;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.*;

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
   * Whether we are loading a matrix for the first time. Necessary for analytics
   * tracking, where we log all matrix loads after the first as a "Change gene
   * set" event.
   * 
   * This is definitely not correct now; see setInitialMatrix.
   */
  private boolean firstMatrixLoad = true;
  /**
   * TODO: Everything this variable touches needs to be thoroughly reconsidered. 
   * 
   * First, this variable seems to represent two, possibly distinct things: 
   * - pathological state when the KCAsyncProvider fails 
   * - states when our data is considered lacking in some way (because we don't have 
   * data, or because we've changed columns, or cleared a table. Not clear if even 
   * this part is a conceptually unified category.) 
   * 
   * The variable is used for a number of things, and it's not clear that the
   * conditions under which these things should happen are identical: 
   * - disabling table refiltering or allowing table row changes (i.e. paging and stuff) 
   * - identifying when we should log (in analytics) that orthologous data was
   * viewed. (This is probably not correct now, because we set loadedData = false
   * every time the dual table is flipped).
   */
  private boolean loadedData = false;

  /**
   * Names of the probes currently displayed
   */
  private String[] displayedAtomicProbes = new String[0];
  /**
   * Names of the (potentially merged) probes being displayed
   */
  private String[] displayedProbes = new String[0];

  private List<ColumnFilter> lastColumnFilters = new ArrayList<ColumnFilter>();

  public interface Delegate {
    void setupColumns();
    void onGettingExpressionFailed();
    void setEnabled(boolean enabled);
    List<Group> chosenColumns();
    void getExpressions();
    SortOrder computeSortParams();
    void onGetRows();
    void onSetRowCount(int numRows);
  }

  interface Loader {
    /**
     * Perform an initial matrix load. The method should load data and then call
     * setInitialMatrix.
     */
    void loadInitialMatrix(ValueType valueType, List<ColumnFilter> initFilters);
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

  public ManagedMatrixInfo info() {
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
    logger.info("Matrix " + matrixId + ":" + msg);
  }

  public void log(Level level, String msg, Throwable throwable) {
    logger.log(level, "Matrix " + matrixId + ":" + msg, throwable);
  }

  // TODO: 
  public void setDirty() {
    loadedData = false;
  }

  /**
   * To be called when a new matrix is set (as opposed to partial refinement or
   * modification of a previously loaded matrix).
   */
  void setInitialMatrix(ManagedMatrixInfo matrix) {
    if (matrix.numRows() > 0) {
      matrixInfo = matrix;
      if (!loadedData) {
        onFirstLoad();
      }
      delegate.setupColumns();
      matrixInfo = matrix;
      setRows(matrix.numRows());

      if (firstMatrixLoad) {
        firstMatrixLoad = false;
      } else {
        /* TODO: This is definitely not correct anymore, because setInitialMatrix can happen as a result
         * of flipping the dual table (which will count an event for both tables), or switching between
         * absolute/folds mode. 
         */
        Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_CHANGE_GENE_SET);
      }

      logInfo("Data successfully loaded");
    } else {
      delegate.onGettingExpressionFailed();
    }
  }

  /**
   * Called when data is successfully loaded for the first time
   */
  private void onFirstLoad() {
    loadedData = true;
    if (matrixInfo.isOrthologous()) {
      Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_VIEW_ORTHOLOGOUS_DATA);
    }
  }

  void removeTests() {
    matrixService.removeSyntheticColumns(matrixId,
        new PendingAsyncCallback<ManagedMatrixInfo>(screen, "There was an error removing the test columns.") {
          @Override
          public void handleSuccess(ManagedMatrixInfo result) {
            matrixInfo = result; // no need to do the full setMatrix
            delegate.setupColumns();
          }
        });
  }

  void addTwoGroupSynthetic(final Synthetic.TwoGroupSynthetic synth, final String name,
      String selectedGroup1, String selectedGroup2) {
    final Group g1 = GroupUtils.findGroup(delegate.chosenColumns(), selectedGroup1).get();
    final Group g2 = GroupUtils.findGroup(delegate.chosenColumns(), selectedGroup2).get();
    synth.setGroups(g1, g2);

    matrixService.addSyntheticColumn(matrixId, synth,
        new PendingAsyncCallback<ManagedMatrixInfo>(screen, "Adding test column failed") {
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
        new PendingAsyncCallback<String>(screen, "Unable to prepare the requested data for download.") {

          @Override
          public void handleSuccess(String url) {
            Utils.displayURL("Your download is ready.", "Download", url);
          }
        });
  }

  protected void editColumnFilter(int column) {
    ColumnFilter filt = matrixInfo.columnFilter(column);
    FilterEditor fe = new FilterEditor(matrixInfo.columnName(column), column, filt) {
      @Override
      protected void onChange(ColumnFilter newVal) {
        applyColumnFilter(editColumn, newVal);
      }
    };
    filterDialog = Utils.displayInPopup("Edit filter", fe, DialogPosition.Center);
  }

  protected void applyColumnFilter(final int column, final @Nullable ColumnFilter filter) {
    delegate.setEnabled(false);
    matrixService.setColumnFilter(matrixId, column, filter, new AsyncCallback<ManagedMatrixInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("An error occurred when the column filter was changed.");
        filterDialog.setVisible(false);
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
          filterDialog.setVisible(false);
        }
      }
    });
  }

  /**
   * Filter data that has already been loaded
   */
  public void refilterData(String[] chosenProbes) {
    if (!loadedData) {
      logInfo("Request to refilter but data was not loaded");
      return;
    }
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
        matrixInfo = result;
        setRows(matrixInfo.numRows());
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
    matrixInfo = null;
    loadedData = false;
    asyncProvider.updateRowCount(0, true);
    delegate.setEnabled(false);
  }

  public void setRows(int numRows) {
    lastColumnFilters = matrixInfo.columnFilters();
    asyncProvider.updateRowCount(numRows, true);
    int initSize = NavigationTools.INIT_PAGE_SIZE;
    int displayRows = (numRows > initSize) ? initSize : numRows;
    delegate.onSetRowCount(displayRows);
    delegate.setEnabled(true);
  }

  /**
   * Load data (when there is nothing stored in our server side session)
   */
  public void getExpressions(boolean preserveFilters, ValueType chosenValueType) {
    delegate.setEnabled(false);
    List<ColumnFilter> initFilters = preserveFilters ? lastColumnFilters : new ArrayList<ColumnFilter>();
    asyncProvider.updateRowCount(0, false);
    loader.loadInitialMatrix(chosenValueType, initFilters);
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
        loadedData = false;
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
        }
      }
    };

    @Override
    protected void onRangeChanged(HasData<ExpressionRow> display) {
      if (loadedData) {
        range = display.getVisibleRange();
        SortOrder order = delegate.computeSortParams();
        if (range.getLength() > 0) {
          matrixService.matrixRows(matrixId, range.getStart(), range.getLength(), order.key, order.asc, rowCallback);
        }
      }
    }
  }
}
