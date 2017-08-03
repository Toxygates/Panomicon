package t.viewer.client.components.search;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

import t.common.client.ImageClickCell;
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
    
    addNonKeyColumn(new ToolColumn(new InspectCell()), "");
  }

  class InspectCell extends ImageClickCell<Unit> {
    InspectCell() {
      super(delegate.inspectCellImage(), false);
    }

    @Override
    protected void appendText(Unit u, SafeHtmlBuilder sb) {}

    @Override
    public void onClick(Unit value) {
      delegate.displayDetailsForEntry(value);
    }
  }

  class ToolColumn extends Column<Unit, Unit> {
    public ToolColumn(InspectCell tc) {
      super(tc);
    }

    @Override
    public Unit getValue(Unit u) {
      return u;
    }
  }
}
