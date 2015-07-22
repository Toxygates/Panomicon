package otgviewer.client.components.groupdef;

import otgviewer.client.components.Screen;
import t.viewer.shared.Unit;

/**
 * A SelTDGrid that ignores the "control" sample count
 *
 */
public class SimpleSelTDGrid extends SelectionTDGrid {

  public SimpleSelTDGrid(Screen screen, UnitListener listener) {
    super(screen, listener);   
  }

  protected class SimpleUnit extends UnitUI {

    SimpleUnit(Unit u) {
      super(u);
    }

    @Override
    protected String unitHoverText() {
      return "Sample count";
    }

    @Override
    protected String unitLabel(int treatedCount, int controlCount) {
      return " " + treatedCount;
    }

  }

  @Override
  protected UnitUI makeUnitUI(Unit unit) {
    return new SimpleUnit(unit);
  }

}
