package t.viewer.client.table;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RequiresResize;

import otgviewer.client.ColumnScreen;
import otgviewer.client.components.Screen;
import t.common.shared.GroupUtils;
import t.common.shared.sample.Group;
import t.viewer.client.components.DataView;
import t.viewer.shared.Association;
import t.viewer.shared.ManagedMatrixInfo;

/**
 * A DataView based on a single ExpressionTable.
 */
public class TableView extends DataView {
  protected ExpressionTable expressionTable;
  
  private Screen screen;
  public TableView(Screen screen,
                   String mainTableTitle, 
                   boolean mainTableSelectable) {
    this.screen = screen;
    this.expressionTable = makeExpressionTable(mainTableTitle, mainTableSelectable);
    this.addListener(expressionTable);
//    initWidget(expressionTable);
  }
  
  protected static final String defaultMatrix = "DEFAULT";
  
  protected ExpressionTable makeExpressionTable(String mainTableTitle, 
                                                boolean mainTableSelectable) {
    TableFlags flags = new TableFlags(defaultMatrix,
        true, true, NavigationTools.INIT_PAGE_SIZE,
        mainTableTitle, mainTableSelectable,
        false);
    
    return new ExpressionTable(screen, flags, TableStyle.getStyle("default")) {
      @Override
      protected void onGettingExpressionFailed() {
        super.onGettingExpressionFailed();
        // If a non-loadable gene list was specified, we try with the blank list
        // (all probes for the species)
        if (chosenProbes.length > 0) {
          TableView.this.probesChanged(new String[0]);
          TableView.this.geneSetChanged(null);
          reloadDataIfNeeded();
        }
        displayInfo("Data loading failed.");
      }
      
      @Override
      protected void associationsUpdated(Association[] result) {
        TableView.this.associationsUpdated(result);
      }
      
      @Override
      public void getAssociations() {
        beforeGetAssociations();
        super.getAssociations();        
      }
      
      @Override
      protected void setMatrix(ManagedMatrixInfo matrix) {
        super.setMatrix(matrix);
        displayInfo("Successfully loaded " + matrix.numRows() + " probes");
      }
    };
  }  
  
  public void reloadDataIfNeeded() {
    logger.info("chosenProbes: " + chosenProbes.length + " lastProbes: "
        + (lastProbes == null ? "null" : "" + lastProbes.length));

    if (chosenColumns.size() == 0) {
      Window.alert("Please define sample groups to see data.");
      screen.manager().attemptProceed(ColumnScreen.key);
      return;
    }
    
    // Attempt to avoid reloading the data
    if (lastColumns == null || !chosenColumns.equals(lastColumns)) {
      logger.info("Data reloading needed");
      expressionTable.setStyle(styleForColumns(chosenColumns));
      expressionTable.getExpressions();      
    } else if (!Arrays.equals(chosenProbes, lastProbes)) {
      logger.info("Only refiltering is needed");
      expressionTable.refilterData();
    }

    lastProbes = chosenProbes;
    lastColumns = chosenColumns;
  }
  
  protected TableStyle styleForColumns(List<Group> columns) {
    boolean foundMirna = false;
    boolean foundNonMirna = false;    
    for (Group g: chosenColumns) {
      if (isMirnaGroup(g)) {      
        foundMirna = true;
      } else {
        foundNonMirna = true;
      }
    }
    
    TableStyle r; 
    if (foundMirna && ! foundNonMirna) {
      r = TableStyle.getStyle("mirna");
    } else {
     r = TableStyle.getStyle("default");
    }
    logger.info("Use table style: " + r);
    return r;    
  }
  
  protected static boolean isMirnaGroup(Group g) {
    return "miRNA".equals(GroupUtils.groupType(g));
  }
  
  public ExpressionTable expressionTable() { return expressionTable; }  
}
