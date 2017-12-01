package otgviewer.client;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import otgviewer.client.components.ScreenManager;
import t.common.shared.AType;
import t.common.shared.GroupUtils;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.table.*;
import t.viewer.shared.*;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;

/**
 * A DataScreen that can display two tables side by side.
 * The dual mode will only activate if appropriate columns have been
 * defined and saved for both tables. Otherwise, the screen will revert to
 * single-table mode.
 */
public class DualDataScreen extends DataScreen {

  protected ExpressionTable sideExpressionTable;
  
  protected final static String sideMatrix = "SECONDARY";
  
  public DualDataScreen(ScreenManager man) {
    super(man);
    
    sideExpressionTable = new ExpressionTable(this, true,
        TableStyle.getStyle("mirna"), sideMatrix);     
    sideExpressionTable.addStyleName("sideExpressionTable");
  }
  
  protected DockLayoutPanel dockLayout;
  
  @Override
  protected Widget mainTablePanel() {
    
    ResizeLayoutPanel rlp = new ResizeLayoutPanel();
    rlp.setWidth("100%");
    dockLayout = new DockLayoutPanel(Unit.PX);    
    dockLayout.addEast(sideExpressionTable, 550);
    dockLayout.add(expressionTable);
    rlp.add(dockLayout);
    return rlp;
  }
  
  protected List<Group> columnsOfType(List<Group> from, String type) {
    return from.stream().filter(g -> type.equals(GroupUtils.groupType(g))).
        collect(Collectors.toList());    
  }
  
  protected List<Group> columnsForMainTable(List<Group> from) {    
    List<Group> r = columnsOfType(from, "mRNA");
    
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<Group> columnsForSideTable(List<Group> from) {
    return columnsOfType(from, "miRNA");        
  }
  
  protected TableStyle mainTableStyle() {
    return super.styleForColumns(columnsForMainTable(chosenColumns));
  }

  protected TableStyle sideTableStyle() {
    return super.styleForColumns(columnsForSideTable(chosenColumns));    
  }
  
  @Override
  protected void changeColumns(List<Group> columns) {    
    super.changeColumns(columnsForMainTable(columns));    
    List<Group> sideColumns = columnsForSideTable(columns);
    if (sideExpressionTable != null && !sideColumns.isEmpty()) {
      sideExpressionTable.columnsChanged(sideColumns);
    }  
    if (!sideColumns.isEmpty()) {
      dockLayout.setWidgetSize(sideExpressionTable, 550);
    } else {
      dockLayout.setWidgetSize(sideExpressionTable, 0);
    }
  }  
  
  @Override
  public void updateProbes() {
    super.updateProbes();
    sideExpressionTable.clearMatrix();
    extractMirnaProbes();
  }
  
  @Override
  protected void associationsUpdated() {
    super.associationsUpdated();
    extractMirnaProbes();
  }
  
  private ColumnFilter lastCountFilter = null;
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    lastCountFilter = countColumnFilter();
    sideExpressionTable.clearMatrix();
  }
  
  protected void extractMirnaProbes() {
    AssociationSummary<ExpressionRow> mirnaSummary = expressionTable.associationSummary(AType.MiRNA);
    if (sideExpressionTable.chosenColumns().isEmpty()) {
      return;
    }
    
    if (mirnaSummary == null) {
      logger.info("Unable to get miRNA summary - not updating side table probes");
      return;
    }
    String[][] rawData = mirnaSummary.getTable();
    if (rawData.length < 2) {
      logger.info("No miRNAs found in summary - not updating side table probes");
      return;
    }
    String[] ids = new String[rawData.length - 1];
    Map<String, Double> counts = new HashMap<String, Double>();
    
    //The first row is headers
    for (int i = 1; i < rawData.length; i++) {    
      ids[i - 1] = rawData[i][1];
      counts.put(rawData[i][1], Double.parseDouble(rawData[i][2]));
    }
    
    logger.info("Extracted " + ids.length + " mirnas");    
    
    Synthetic.Precomputed countColumn = new Synthetic.Precomputed("Count", 
      "Number of times each miRNA appeared", counts,
      lastCountFilter);

    List<Synthetic> synths = new ArrayList<Synthetic>();
    synths.add(countColumn);
    
    changeSideTableProbes(ids, synths);
  }
  
  private @Nullable ColumnFilter countColumnFilter() {
    ManagedMatrixInfo matInfo = sideExpressionTable.currentMatrixInfo();
    if (matInfo == null) {
      return null;
    }
    int numData = matInfo.numDataColumns();
    int numSynth = matInfo.numSynthetics();
    if (numData > 0 && numSynth > 0) {
      //We rely on the count column being the first synthetic
      return matInfo.columnFilter(numData);
    } else {
      return null;
    }
  }
  
  
  protected void changeSideTableProbes(String[] probes, List<Synthetic> synths) {      
    sideExpressionTable.probesChanged(probes);
    if (probes.length > 0) {
      sideExpressionTable.getExpressions(synths);
    }
  }
}
