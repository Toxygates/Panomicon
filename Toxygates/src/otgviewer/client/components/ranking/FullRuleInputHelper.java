/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.shared.RankRule;
import otgviewer.shared.RuleType;
import t.model.SampleClass;
import static otg.model.sample.OTGAttribute.*;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;

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

  final static int REQUIRED_COLUMNS = 6;

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
    SampleClass sc = ranker.chosenSampleClass.copy();
    sc.put(Compound, selCompound);

    ranker.sampleService.parameterValues(sc, DoseLevel.id(), new PendingAsyncCallback<String[]>(
        ranker.selector, "Unable to retrieve dose levels.") {

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
        SampleClass sc = ranker.chosenSampleClass;
        int expectedPoints = ranker.schema.numDataPointsInSeries(sc);

        if (ss.length != expectedPoints) {
          throw new RankRuleException("Please supply " + expectedPoints
              + " space-separated values as the synthetic curve." + " (Example: 1 -2 ...)");
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

  void sampleClassChanged(SampleClass sc) {
    refCompound.clear();
    refDose.clear();
  }

  void availableCompoundsChanged(List<String> compounds) {
    refCompound.clear();
    refDose.clear();
    for (String c : compounds) {
      refCompound.addItem(c);
    }
    updateDoses();
  }
}
