package t.viewer.client.table;

import java.util.List;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.common.shared.ValueType;
import t.common.shared.sample.DataColumn;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.shared.Synthetic;

/**
 * GUI for controlling two-group comparisons in an ExpressionTable.
 */
class AnalysisTools extends Composite {
  /**
   * For selecting sample groups to apply two-column comparisons to
   */
  private ListBox groupsel1 = new ListBox(), groupsel2 = new ListBox();
  
  // We enable/disable this button when the value type changes
  private Button foldChangeBtn;

  private HorizontalPanel analysisTools;
  
  AnalysisTools(ExpressionTable table) {
    analysisTools = Utils.mkHorizontalPanel(true);
    initWidget(analysisTools);
    
    analysisTools.addStyleName("analysisTools");

    analysisTools.add(groupsel1);
    groupsel1.setVisibleItemCount(1);
    analysisTools.add(groupsel2);
    groupsel2.setVisibleItemCount(1);

    analysisTools.add(new Button("Add T-test", (ClickHandler) e -> {
      if (twoGroupCheck("T-test")) {
        table.addTwoGroupSynthetic(new Synthetic.TTest(null, null), "T-test");
        Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ADD_COMPARISON_COLUMN,
            Analytics.LABEL_T_TEST);
      }
    }));

    analysisTools.add(new Button("Add U-test", (ClickHandler) e -> {
      if (twoGroupCheck("U-test")) {
        table.addTwoGroupSynthetic(new Synthetic.UTest(null, null), "U-test");
        Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ADD_COMPARISON_COLUMN,
            Analytics.LABEL_U_TEST);
      }
    }));

    foldChangeBtn = new Button("Add fold-change difference");
    foldChangeBtn.addClickHandler(e -> {
      table.addTwoGroupSynthetic(new Synthetic.MeanDifference(null, null), "Fold-change difference");
      Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ADD_COMPARISON_COLUMN,
          Analytics.LABEL_FOLD_CHANGE_DIFFERENCE);
    });
    analysisTools.add(foldChangeBtn);

    analysisTools.add(new Button("Remove tests", (ClickHandler) e -> {
      table.removeTests();
    }));
    analysisTools.setVisible(false); // initially hidden
  }
  
  private boolean twoGroupCheck(String testName) {
    if (groupsel1.getSelectedIndex() == -1 || groupsel2.getSelectedIndex() == -1) {
      Window.alert("Please select two groups to compute " + testName + ".");
      return false;
    } else if (groupsel1.getSelectedIndex() == groupsel2.getSelectedIndex()) {
      Window.alert("Please select two different groups to perform " + testName + ".");
      return false;
    } 
    return true;
  }
  
  String selectedGroup1() {
    return groupsel1.getItemText(groupsel1.getSelectedIndex());
  }
  
  String selectedGroup2() {
    return groupsel2.getItemText(groupsel2.getSelectedIndex());
  }
  
  void setEnabled(ValueType valueType, boolean newState) {
    Utils.setEnabled(analysisTools, newState);
    switch (valueType) {
      case Absolute:
        foldChangeBtn.setEnabled(false);
        break;
      case Folds:
        foldChangeBtn.setEnabled(newState);
        break;
    }
  }
  
  /**
   * Re-initialise this UI when the available columns changed.
   * @param columns
   */
  void columnsChanged(List<Group> columns) {
    groupsel1.clear();
    groupsel2.clear();
    for (DataColumn<?> dc : columns) {
      if (dc instanceof Group) {
        groupsel1.addItem(dc.getShortTitle());
        groupsel2.addItem(dc.getShortTitle());
      }
    }

    if (columns.size() >= 2) {
      groupsel1.setSelectedIndex(0);
      groupsel2.setSelectedIndex(1);
    }
  }
}
