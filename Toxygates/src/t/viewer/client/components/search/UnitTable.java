package t.viewer.client.components.search;

import t.common.client.ImageClickCell;
import t.common.shared.sample.Unit;
import t.model.sample.Attribute;
import t.model.sample.CoreParameter;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

public class UnitTable extends ResultTable<Unit> {
  private TextCell textCell = new TextCell();

  public UnitTable(Delegate delegate) {
    super(delegate);
  }

  protected class UnitAttributeColumn extends AttributeColumn<Unit> {
    public UnitAttributeColumn(Cell<String> cell, Attribute attribute, boolean numeric) {
      super(cell, attribute, numeric);
    }

    @Override
    public String getData(Unit unit) {
      return unit.get(attribute);
    }
  }

  @Override
  protected AttributeColumn<Unit> makeColumn(Attribute attribute, boolean numeric) {
    return new UnitAttributeColumn(textCell, attribute, numeric);
  }

  @Override
  protected void addAdhocColumns() {
    addAttributeColumn(new UnitAttributeColumn(textCell, CoreParameter.SampleId, false) {
      @Override
      public String getValue(Unit unit) {
        return getData(unit).split("\\s*/\\s*")[0] + "...";
      }
    }, CoreParameter.SampleId);
    
    addNonAttributeColumn(new ToolColumn(new InspectCell()), "");
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
