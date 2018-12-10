/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otg.viewer.client.screen.ranking;

import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;

import otg.model.sample.OTGAttribute;
import otg.viewer.shared.RankRule;
import otg.viewer.shared.RuleType;
import t.common.shared.SeriesType;
import t.model.SampleClass;
import t.viewer.client.components.PendingAsyncCallback;

public class FullRuleInputHelper extends RuleInputHelper {

  final ListBox refDose = new ListBox();
  final TextBox syntheticCurveText = new TextBox();
  final ListBox refCompound = new ListBox();

  public FullRuleInputHelper(CompoundRanker _ranker, boolean lastRule) {
    super(_ranker, lastRule);

    syntheticCurveText.setWidth("5em");
    syntheticCurveText.setEnabled(false);

    refCompound.setStylePrimaryName("colored");
    refCompound.setEnabled(false);
    for (String c : ranker.availableCompounds) {
      refCompound.addItem(c);
    }

    refCompound.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        updateDoses();
      }
    });

    refDose.setStylePrimaryName("colored");
    refDose.setEnabled(false);
  }
  
  protected SeriesType currentRankingMode() {
    return ranker.rankingType();
  }

  final static int REQUIRED_COLUMNS = 6;

  @Override
  protected RuleType[] ruleTypes() {
    return RuleType.values();
  }

  @Override
  void populate(Grid grid, int row) {
    super.populate(grid, row);
    grid.setWidget(row + 1, 3, syntheticCurveText);
    grid.setWidget(row + 1, 4, refCompound);
    grid.setWidget(row + 1, 5, refDose);
  }

  void updateDoses() {
    refDose.clear();
    int selIndex = refCompound.getSelectedIndex();
    if (selIndex == -1) {
      return;
    }
    final String selCompound = refCompound.getItemText(selIndex);
    SampleClass sc = ranker.sampleClass();
    sc.put(OTGAttribute.Compound, selCompound);

    ranker.sampleService.parameterValues(sc, OTGAttribute.DoseLevel.id(),
        new PendingAsyncCallback<String[]>(ranker.screen, "Unable to retrieve dose levels.") {

      @Override
      public void handleSuccess(String[] result) {
        for (String i : result) {
          if (!i.equals("Control")) {
            refDose.addItem(i);
          }
        }
      }
    });
  }

  @Override
  protected void rankTypeChanged() {
    RuleType rt = rankType.value();
    switch (rt) {
      case Synthetic:
        syntheticCurveText.setEnabled(true);
        refCompound.setEnabled(false);
        refDose.setEnabled(false);
        break;
      case ReferenceCompound:
        syntheticCurveText.setEnabled(false);
        refCompound.setEnabled(true);
        if (refCompound.getItemCount() > 0) {
          refCompound.setSelectedIndex(0);
          updateDoses();
        }
        refDose.setEnabled(true);
        break;
      default:
        syntheticCurveText.setEnabled(false);
        refCompound.setEnabled(false);
        refDose.setEnabled(false);
        break;
    }
  }

  @Override
  void copyFrom(RuleInputHelper other) throws RankRuleException {
    super.copyFrom(other);
    if (other instanceof FullRuleInputHelper) {
      FullRuleInputHelper otherInput = (FullRuleInputHelper) other;
      syntheticCurveText.setText(otherInput.syntheticCurveText.getText());
      refDose.setSelectedIndex(otherInput.refDose.getSelectedIndex());
      refCompound.setSelectedIndex(otherInput.refCompound.getSelectedIndex());
    } else {
      throw new IllegalArgumentException("Expected FullRuleInputHelper");
    }
  }

  @Override
  RankRule getRule() throws RankRuleException {
    RuleType rt = rankType.value();
    String probe = probeText.getText();
    switch (rt) {
      case Synthetic: {
        double[] data;
        String[] ss = syntheticCurveText.getText().split(" ");
        RankRule r = new RankRule(rt, probe);
        SampleClass sc = ranker.sampleClass();
        final int expectedPoints = 
            ranker.schema.numDataPointsInSeries(sc, currentRankingMode());

        if (ss.length < expectedPoints) {
          throw new RankRuleException("Please supply " + expectedPoints
              + " space-separated values as the synthetic curve." + " (Example: '1 -2 3' ...)");
        } else {
          data = new double[expectedPoints];
          for (int j = 0; j < ss.length; ++j) {
            data[j] = Double.valueOf(ss[j]);
          }
          r.setData(data);
          return r;
        }
      }
      case ReferenceCompound: {
        String cmp = refCompound.getItemText(refCompound.getSelectedIndex());
        RankRule r = new RankRule(rt, probe);
        r.setCompound(cmp);
        r.setDose(refDose.getItemText(refDose.getSelectedIndex()));
        return r;
      }
      default:
        return super.getRule();
    }
  }

  @Override
  void reset() {
    super.reset();
    refCompound.setSelectedIndex(0);
    syntheticCurveText.setText("");
  }

  @Override
  void availableCompoundsChanged(List<String> compounds) {
    refCompound.clear();
    refDose.clear();
    for (String c : compounds) {
      refCompound.addItem(c);
    }
    updateDoses();
  }
}
