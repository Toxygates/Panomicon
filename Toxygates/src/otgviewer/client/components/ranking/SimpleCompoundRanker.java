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

package otgviewer.client.components.ranking;

import otgviewer.client.components.DLWScreen;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import t.viewer.client.Utils;

public class SimpleCompoundRanker extends CompoundRanker {

  public SimpleCompoundRanker(DLWScreen _screen, RankingCompoundSelector selector) {
    super(_screen, selector);
  }

  protected void addHeaderWidgets() {
    grid.setWidget(0, 1, Utils.mkEmphLabel("Gene/probe"));
    grid.setWidget(0, 2, Utils.mkEmphLabel("Match type"));
  }

  protected int gridColumns() {
    return SimpleRuleInputHelper.REQUIRED_COLUMNS;
  }

  protected RuleInputHelper makeInputHelper(boolean isLast) {
    return new SimpleRuleInputHelper(this, isLast);
  }

}
