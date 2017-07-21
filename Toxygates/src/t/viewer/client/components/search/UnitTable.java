package t.viewer.client.components.search;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;

import t.common.shared.sample.Unit;
import t.viewer.client.table.TooltipColumn;

public class UnitTable extends ResultTable<Unit> {
  private TextCell textCell = new TextCell();

  public UnitTable(Delegate delegate) {
    super(delegate);
  }

  protected class UnitKeyColumn extends KeyColumn<Unit> {
    public UnitKeyColumn(Cell<String> cell, String key) {
      super(cell, key);
    }

    @Override
    public String getData(Unit unit) {
      return unit.get(keyName);
    }
  }

  private UnitKeyColumn makeColumn(String key) {
    return new UnitKeyColumn(textCell, key);
  }

  @Override
  protected TooltipColumn<Unit> makeBasicColumn(String key) {
    return makeColumn(key);
  }

  @Override
  protected TooltipColumn<Unit> makeNumericColumn(String key) {
    return makeColumn(key);
  }

  @Override
  protected void addAdhocColumns() {
    addColumn(new UnitKeyColumn(textCell, "sample_id") {
      @Override
      public String getValue(Unit unit) {
        return getData(unit).split("\\s*/\\s*")[0];
      }
    }, "sample_id");
  }
}
