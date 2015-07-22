package otgviewer.client.components.groupdef;

import otgviewer.client.components.Screen;
import t.viewer.shared.Unit;

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
