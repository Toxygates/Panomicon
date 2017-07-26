package t.viewer.client.components.search;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;

import t.common.shared.sample.Unit;

public class UnitTable extends ResultTable<Unit> {
  private TextCell textCell = new TextCell();

  public UnitTable(Delegate delegate) {
    super(delegate);
  }

  protected class UnitKeyColumn extends KeyColumn<Unit> {
    public UnitKeyColumn(Cell<String> cell, String key, boolean numeric) {
      super(cell, key, numeric);
    }

    @Override
    public String getData(Unit unit) {
      return unit.get(keyName);
    }
  }

  @Override
  protected KeyColumn<Unit> makeColumn(String key, boolean numeric) {
    return new UnitKeyColumn(textCell, key, numeric);
  }

  @Override
  protected void addAdhocColumns() {
    addColumn(new UnitKeyColumn(textCell, "sample_id", false) {
      @Override
      public String getValue(Unit unit) {
        return getData(unit).split("\\s*/\\s*")[0];
      }
    }, "sample_id");
  }
}
