/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.components.ranking;

import java.util.List;

import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;
import t.common.client.components.EnumSelector;
import t.common.shared.SampleClass;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SuggestBox;

/**
 * Data and widgets that help the user input a rule but do not need to be sent to the server when
 * the ranking is performed.
 */
abstract class RuleInputHelper {
  private boolean isLastRule;

  final RankRule rule;

  final SuggestBox probeText;

  final CheckBox enabled = new CheckBox();
  final EnumSelector<RuleType> rankType = new EnumSelector<RuleType>() {
    protected RuleType[] values() {
      return ruleTypes();
    }
  };
  final CompoundRanker ranker;

  abstract protected RuleType[] ruleTypes();

  RuleInputHelper(CompoundRanker _ranker, RankRule r, boolean lastRule) {
    rule = r;
    this.isLastRule = lastRule;
    this.ranker = _ranker;
    probeText = new SuggestBox(ranker.oracle);

    rankType.listBox().addChangeHandler(rankTypeChangeHandler());
    probeText.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        enabled.setValue(true);
        if (isLastRule) {
          ranker.addRule(true);
          isLastRule = false;
        }
      }
    });
  }

  ChangeHandler rankTypeChangeHandler() {
    return new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        rankTypeChanged();
      }
    };
  }

  protected void rankTypeChanged() {

  }

  void populate(Grid grid, int row) {
    grid.setWidget(row + 1, 0, enabled);
    grid.setWidget(row + 1, 1, probeText);
    grid.setWidget(row + 1, 2, rankType);
  }

  void reset() {
    rankType.reset();
    probeText.setText("");
    enabled.setValue(false);
  }

  RankRule getRule() throws RankRuleException {
    String probe = probeText.getText();
    RuleType rt = rankType.value();
    return new RankRule(rt, probe);
  }

  void sampleClassChanged(SampleClass sc) {

  }

  void availableCompoundsChanged(List<String> compounds) {

  }
}
