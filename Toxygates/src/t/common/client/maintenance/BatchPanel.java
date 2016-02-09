package t.common.client.maintenance;

import t.common.client.ImageClickCell;
import t.common.client.Resources;
import t.common.client.components.StringArrayTable;
import t.common.client.rpc.BatchOperationsAsync;
import t.common.shared.maintenance.Batch;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

abstract public class BatchPanel extends ManagerPanel<Batch> {

  protected final BatchOperationsAsync batchOps;
  
  public BatchPanel(String editCaption, BatchOperationsAsync batchOps, Resources resources) {
    super(editCaption, resources);
    this.batchOps = batchOps;
  }
  
  abstract protected void doRefresh();

  abstract protected Widget makeEditor(Batch b, final DialogBox db, boolean addNew);

  @Override 
  protected void addMidColumns(CellTable<Batch> table) {
    TextColumn<Batch> samplesColumn = new TextColumn<Batch>() {
      @Override
      public String getValue(Batch object) {
        return "" + object.getNumSamples();
      }
    };

    table.addColumn(samplesColumn, "Samples");
    table.setColumnWidth(samplesColumn, "6em");

    //TODO factor out column construction code, share with e.g. PathologyScreen
    final ImageClickCell<String> overviewCell = 
        new ImageClickCell.StringImageClickCell(resources.magnify(), false) {

      @Override
      public void onClick(final String value) {
        batchOps.batchParameterSummary(new Batch(value, ""), 
            new AsyncCallback<String[][]>() {
              @Override
              public void onFailure(Throwable caught) {
                Window.alert("Unable to obtain batch data");                    
              }

              @Override
              public void onSuccess(String[][] result) {
                showBatchOverview(value, result);                    
              }              
        });
      }          
    };
    class InspectColumn extends Column<Batch, String> {        
      public InspectColumn() {
          super(overviewCell);          
      }
      
      public String getValue(Batch b) {
          return b.getTitle();         
      }
    }
    InspectColumn ic = new InspectColumn();
    table.addColumn(ic, "");
    table.setColumnWidth(ic, "40px");
    ic.setCellStyleNames("clickCell");
    
    TextColumn<Batch> dsColumn = new TextColumn<Batch>() {
      @Override
      public String getValue(Batch object) {
        return "" + object.getDataset();
      }
    };
    table.addColumn(dsColumn, "Dataset");
    table.setColumnWidth(dsColumn, "8em");

    TextColumn<Batch> visibilityColumn = new TextColumn<Batch>() {
      @Override
      public String getValue(Batch object) {
        StringBuilder sb = new StringBuilder();
        for (String inst : object.getEnabledInstances()) {
          sb.append(inst);
          sb.append(", ");
        }
        String r = sb.toString();
        if (r.length() > 2) {
          return r.substring(0, r.length() - 2);
        } else {
          return "";
        }
      }
    };

    table.addColumn(visibilityColumn, "Visibility");
  }

  protected void showBatchOverview(String title, String[][] data) {
    StringArrayTable.displayDialog(data, "Overview for for batch " + title, 
        800, 600);    
  }
}
