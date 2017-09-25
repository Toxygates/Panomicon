package otgviewer.client.dialog;

import java.util.Arrays;

import t.common.client.components.SelectionTable;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;

/**
 * A dialog for selecting among a set of available miRNA sources and,
 * optionally, setting thresholds and selecting a source combination method.
 */
public class MirnaSourceSelector extends SelectionTable<MirnaSource> {
  MirnaSource[] availableSources;
  
  public MirnaSourceSelector(MirnaSource[] availableSources) {
    super("", false);
    this.availableSources = availableSources;
    setItems(Arrays.asList(availableSources));
  }
  
  /**
   * @return The current selections. Where appropriate, the limit field
   * will be set according to the user's wishes.
   */
  public MirnaSource[] getSelections() {
    return availableSources;
  }

  @Override
  protected void initTable(CellTable<MirnaSource> table) {
    TextColumn<MirnaSource> textColumn = new TextColumn<MirnaSource>() {
      @Override
      public String getValue(MirnaSource object) {
        return object.title();
      }
    };
    table.addColumn(textColumn, "Source title");    
    
    textColumn = new TextColumn<MirnaSource>() {
      @Override
      public String getValue(MirnaSource object) {
        if (object.empirical()) {
          return "Yes";
        } else {
          return "No";
        }
      }
    };
    table.addColumn(textColumn, "Empirically validated");    
    
    textColumn = new TextColumn<MirnaSource>() {
      @Override
      public String getValue(MirnaSource object) {
        return object.limit() + "";        
      }
    };
    table.addColumn(textColumn, "Limit");    
    
    textColumn = new TextColumn<MirnaSource>() {
      @Override
      public String getValue(MirnaSource object) {
        return object.size() + "";        
      }
    };
    table.addColumn(textColumn, "# of associations");    
    
  }
}
