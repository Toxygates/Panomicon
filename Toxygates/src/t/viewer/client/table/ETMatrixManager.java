package t.viewer.client.table;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.GroupUtils;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.FilterEditor;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.*;

/**
 * Manages a ManagedMatrixInfo,
 */
public class ETMatrixManager {

  private Screen screen;
  private final MatrixServiceAsync matrixService;
  private String matrixId;
  public ManagedMatrixInfo matrixInfo = null;
  // For Analytics: we count every matrix load other than the first as a gene set change
  private boolean firstMatrixLoad = true;

  private boolean loadedData = false;

  private final Logger logger = SharedUtils.getLogger("matrixManager");
  private Delegate delegate;

  private DialogBox filterDialog = null;

  public interface Delegate {
    void setupColumns();
    void setRows(int numRows);

    void onGettingExpressionFailed();

    void setEnabled(boolean enabled);

    List<Group> chosenColumns();

    void getExpressions();
  }

  public ETMatrixManager(Screen screen, TableFlags flags) {
    this.screen = screen;
    this.matrixService = screen.manager().matrixService();
    this.matrixId = flags.matrixId;
  }

  public ManagedMatrixInfo info() {
    return matrixInfo;
  }

  public String id() {
    return matrixId;
  }

  public boolean loadedData() {
    return loadedData;
  }

  public void clear() {
    matrixInfo = null;
    loadedData = false;
  }

  public List<ColumnFilter> columnFilters() {
    return matrixInfo.columnFilters();
  }

  public String[] displayedAtomicProbes() {
    String[] r = matrixInfo.getAtomicProbes();
    if (r.length < matrixInfo.numRows()) {
      Window.alert("Too many genes. Only the first " + r.length + " genes will be used.");
    }
    return r;
  }

  protected boolean hasPValueColumns() {
    for (int i = 0; i < matrixInfo.numDataColumns(); ++i) {
      if (matrixInfo.isPValueColumn(i)) {
        return true;
      }
    }
    return false;
  }

  public void logInfo(String msg) {
    logger.info("Matrix " + matrixId + ":" + msg);
  }

  public void log(Level level, String msg, Throwable throwable) {
    logger.log(level, "Matrix " + matrixId + ":" + msg, throwable);
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
      delegate.setRows(matrix.numRows());

      if (firstMatrixLoad) {
        firstMatrixLoad = false;
      } else {
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
            delegate.setRows(r.numRows());
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
          delegate.setRows(matrixInfo.numRows());
          delegate.setupColumns();
          filterDialog.setVisible(false);
        }
      }
    });
  }

  public void refilterData(String[] chosenProbes) {
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
        delegate.setRows(matrixInfo.numRows());
      }
    });
  }
}
