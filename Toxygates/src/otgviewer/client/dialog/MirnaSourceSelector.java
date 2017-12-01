package otgviewer.client.dialog;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import t.common.client.components.SelectionTable;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;

/**
 * A dialog for selecting among a set of available miRNA sources and,
 * optionally, setting thresholds and selecting a source combination method.
 */
public class MirnaSourceSelector extends SelectionTable<MirnaSource> {
  MirnaSource[] availableSources;
  
  /**
   * @param availableSources
   * @param preferredSources Sources that are already selected. Only IDs and
   * cutoff values will be respected.
   */
  public MirnaSourceSelector(MirnaSource[] availableSources,
                             @Nullable MirnaSource[] preferredSources) {
    super("", false);
    this.availableSources = availableSources;
    
    setItems(Arrays.asList(availableSources));
    if (preferredSources != null) {
      Map<String, Double> preferred = 
          Arrays.stream(preferredSources).collect(Collectors.toMap(ms -> ms.id(), ms -> ms.limit()));
      for (MirnaSource s: availableSources) {
        if (preferred.containsKey(s.id())) {
          s.setLimit(preferred.get(s.id()));
          select(s);
        }
      }
    }    
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
        if (object.experimental()) {
          return "Yes";
        } else {
          return "No";
        }
      }
    };
    table.addColumn(textColumn, "Experimentally validated");    
    
    Column <MirnaSource, String> scoreColumn = new Column<MirnaSource, String>(new EditTextCell()) {    
      @Override
      public String getValue(MirnaSource object) {
        return object.limit() + "";        
      }
    };
    
    table.addColumn(scoreColumn, "Score limit (lower)");    
    scoreColumn.setFieldUpdater(new FieldUpdater<MirnaSource, String>() {      
      @Override
      public void update(int row, MirnaSource source, String value) {
        try {
          source.setLimit(Double.valueOf(value));
        } catch (NumberFormatException e) {
          Window.alert("Please enter a numerical value as the limit.");
        }
      }
    });
    
    textColumn = new TextColumn<MirnaSource>() {
      @Override
      public String getValue(MirnaSource object) {
        return object.size() + "";        
      }
    };
    table.addColumn(textColumn, "# of associations");    
    
  }
}
