/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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
package otgviewer.client.components.groupdef;

import otgviewer.client.components.Screen;
import t.common.shared.sample.Unit;

/**
 * A SelTDGrid that displays counts of both treated and control samples.
 *
 */
public class TreatedControlSelTDGrid extends SelectionTDGrid {

  public TreatedControlSelTDGrid(Screen screen, UnitListener listener) {
    super(screen, listener);
  }

  protected class TreatedControlUnit extends UnitUI {
    TreatedControlUnit(Unit u) {
      super(u);
    }

    @Override
    protected String unitHoverText() {
      return "Treated samples/Control samples";
    }

    private String format(int x) {
      return x == -1 ? "?" : (x + "");
    }
    
    @Override
    protected String unitLabel(int treatedCount, int controlCount) {      
      return " " + format(treatedCount) + "/" + format(controlCount);
    }

  }

  @Override
  protected UnitUI makeUnitUI(Unit unit) {
    return new TreatedControlUnit(unit);
  }
}
