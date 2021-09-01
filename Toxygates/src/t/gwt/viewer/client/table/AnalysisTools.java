/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.viewer.client.table;

import java.util.List;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.shared.common.ValueType;
import t.shared.common.sample.DataColumn;
import t.shared.common.sample.Group;
import t.gwt.viewer.client.Analytics;
import t.gwt.viewer.client.ClientGroup;
import t.gwt.viewer.client.Utils;
import t.shared.viewer.Synthetic;

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
        table.matrix().addTwoGroupSynthetic(new Synthetic.TTest(null, null), "T-test",
            selectedGroup1(), selectedGroup2());
        Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ADD_COMPARISON_COLUMN,
            Analytics.LABEL_T_TEST);
      }
    }));

    analysisTools.add(new Button("Add U-test", (ClickHandler) e -> {
      if (twoGroupCheck("U-test")) {
        table.matrix().addTwoGroupSynthetic(new Synthetic.UTest(null, null), "U-test",
            selectedGroup1(), selectedGroup2());
        Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ADD_COMPARISON_COLUMN,
            Analytics.LABEL_U_TEST);
      }
    }));

    foldChangeBtn = new Button("Add fold-change difference");
    foldChangeBtn.addClickHandler(e -> {
      table.matrix().addTwoGroupSynthetic(new Synthetic.MeanDifference(null, null), "Fold-change difference",
          selectedGroup1(), selectedGroup2());
      Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_ADD_COMPARISON_COLUMN,
          Analytics.LABEL_FOLD_CHANGE_DIFFERENCE);
    });
    analysisTools.add(foldChangeBtn);

    analysisTools.add(new Button("Remove tests", (ClickHandler) e -> {
      table.matrix().removeTests();
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
   */
  void columnsChanged(List<ClientGroup> columns) {
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
