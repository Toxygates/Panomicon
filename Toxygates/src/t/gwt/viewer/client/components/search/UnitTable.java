/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.gwt.viewer.client.components.search;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

import t.gwt.common.client.ImageClickCell;
import t.shared.common.sample.Unit;
import t.model.sample.Attribute;
import t.model.sample.CoreParameter;

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
      super(delegate.inspectCellImage(), "inspect", false);
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
      setCellStyleNames("lightBorderLeft");
    }

    @Override
    public Unit getValue(Unit u) {
      return u;
    }
  }
}
