package otgviewer.client;

import java.util.*;

import otgviewer.client.components.ScreenManager;
import t.common.shared.AType;
import t.common.shared.GroupUtils;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.table.*;

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
  
  public DualDataScreen(ScreenManager man) {
    super(man);
    
    sideExpressionTable = new ExpressionTable(this, true, TableStyle.getStyle("mirna"));      
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
  
  //TODO if columns are not mixed, we should simply return all columns here, even if
  //they are mirna
  protected List<Group> columnsForMainTable(List<Group> from) {
    ArrayList<Group> r = new ArrayList<Group>();
    for (Group g: from) {
      if (!GroupUtils.isMirnaGroup(g)) {
        r.add(g);
      }
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
    extractMirnaProbes();
  }
  
  @Override
  protected void associationsUpdated() {
    super.associationsUpdated();
    extractMirnaProbes();
  }
  
  protected void extractMirnaProbes() {
    AssociationSummary<ExpressionRow> mirnaSummary = expressionTable.associationSummary(AType.MiRNA);
    if (mirnaSummary == null) {
      logger.info("Unable to get miRNA summary - not updating side table probes");
      return;
    }
    String[][] rawData = mirnaSummary.getTable();
    String[] ids = new String[rawData.length];
    Map<String, String> counts = new HashMap<String, String>();
    for (int i = 0; i < rawData.length; i++) {    
      ids[i] = rawData[i][1];
      counts.put(rawData[i][1], rawData[i][2]);
    }
    
    logger.info("Extracted " + ids.length + " mirnas");
    sideExpressionTable.setStaticAssociation(StandardColumns.Count.toString(), counts);
    changeSideTableProbes(ids);
  }
  
  protected void changeSideTableProbes(String[] probes) {      
    sideExpressionTable.probesChanged(probes);
    sideExpressionTable.getExpressions();
  }
}
