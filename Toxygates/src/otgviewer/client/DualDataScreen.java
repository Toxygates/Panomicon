package otgviewer.client;

import java.util.*;

import otgviewer.client.components.ScreenManager;
import t.common.shared.AType;
import t.common.shared.GroupUtils;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.table.*;
import t.viewer.shared.Synthetic;

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
  }
  
  @Override
  protected Widget mainTablePanel() {
    
    ResizeLayoutPanel rlp = new ResizeLayoutPanel();
    rlp.setWidth("100%");
    DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);    
    dlp.addEast(sideExpressionTable, 400);
    dlp.add(expressionTable);
    rlp.add(dlp);
    return rlp;
  }
  
  protected List<Group> columnsForMainTable(List<Group> from) {
    ArrayList<Group> r = new ArrayList<Group>();
    for (Group g: from) {
      if (!GroupUtils.isMirnaGroup(g)) {
        r.add(g);
      }
    }
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<Group> columnsForSideTable(List<Group> from) {
    ArrayList<Group> r = new ArrayList<Group>();
    for (Group g: from) {
      if (GroupUtils.isMirnaGroup(g)) {
        r.add(g);
      }
    }
    return r;
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
    if (sideExpressionTable != null) {
      sideExpressionTable.columnsChanged(columnsForSideTable(columns));
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
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    sideExpressionTable.clearMatrix();
  }
  
  protected void extractMirnaProbes() {
    AssociationSummary<ExpressionRow> mirnaSummary = expressionTable.associationSummary(AType.MiRNA);
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
      "Number of times each miRNA appeared", counts);

    List<Synthetic> synths = new ArrayList<Synthetic>();
    synths.add(countColumn);
    
    changeSideTableProbes(ids, synths);
  }
  
  protected void changeSideTableProbes(String[] probes, List<Synthetic> synths) {      
    sideExpressionTable.probesChanged(probes);
    if (probes.length > 0) {
      sideExpressionTable.getExpressions(synths);
    }
  }
}
