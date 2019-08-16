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

package t.viewer.client.components.search;

import java.util.*;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.*;

import otg.model.sample.OTGAttribute;
import t.common.client.Utils;
import t.common.client.components.SelectionTable;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.model.sample.Attribute;
import t.model.sample.CoreParameter;
import t.viewer.client.table.TooltipColumn;

/**
 * Manages a table for displaying the results of a sample/unit search.
 */
public abstract class ResultTable<T> {
  public interface Delegate {
    void finishedSettingUpTable();
    ImageResource inspectCellImage();
    void displayDetailsForEntry(Unit unit);
  }

  protected SelectionTable<T> selectionTable = new SelectionTable<T>("", false) {
    @Override
    protected void initTable(CellTable<T> table) {}
  };
  private Map<Attribute, AttributeColumn<T>> attributeColumns = new HashMap<Attribute, AttributeColumn<T>>();
  private List<Column<T, ?>> nonAttributeColumns = new ArrayList<Column<T, ?>>();
  private List<Attribute> conditionAttributes = new ArrayList<Attribute>();
  private List<Attribute> additionalAttributes = new LinkedList<Attribute>();
  protected Delegate delegate;

  protected abstract AttributeColumn<T> makeColumn(Attribute attribute, boolean numeric);

  // Could also have macro parameters here, such as organism, tissue etc
  // but currently the search is always constrained on those parameters
  private final Attribute[] classAttributes =
      {OTGAttribute.Compound, OTGAttribute.DoseLevel, OTGAttribute.ExposureTime};
  private final Attribute[] adhocAttributes = {CoreParameter.SampleId};

  public ResultTable(Delegate delegate) {
    this.delegate = delegate;
  }

  public SelectionTable<T> selectionTable() {
    return selectionTable;
  }

  public CellTable<T> cellTable() {
    return selectionTable.table();
  }

  public Attribute[] allAttributes() {
    List<Attribute> attributes = new ArrayList<Attribute>();
    attributes.addAll(Arrays.asList(requiredAttributes()));
    attributes.addAll(Arrays.asList(nonRequiredAttributes()));

    return attributes.toArray(new Attribute[0]);
  }

  /**
   * Attributes that cannot be hidden
   */
  public Attribute[] requiredAttributes() {
    List<Attribute> attributes = new ArrayList<Attribute>();
    attributes.addAll(Arrays.asList(classAttributes()));
    attributes.addAll(Arrays.asList(adhocAttributes()));
    attributes.addAll(conditionAttributes);

    return attributes.toArray(new Attribute[0]);
  }

  /**
   * Attributes that can be hidden
   */
  public Attribute[] nonRequiredAttributes() {
    return additionalAttributes.toArray(new Attribute[0]);
  }

  /**
   * Attributes that identify the sample class
   */
  protected Attribute[] classAttributes() {
    return classAttributes;
  }

  /**
   * Other required Attributes
   */
  protected Attribute[] adhocAttributes() {
    return adhocAttributes;
  }

  private void setConditionAttributes(MatchCondition condition) {
    assert(conditionAttributes.size() == 0);  
    for (Attribute attr : condition.neededParameters()) {
      conditionAttributes.add(attr);
    }
  }

  public void addExtraColumn(Attribute attribute, boolean isNumeric, boolean waitForData) {
    addNewColumn(attribute, isNumeric, waitForData);
    additionalAttributes.add(attribute);
  }

  private void addNewColumn(Attribute attribute, boolean isNumeric, boolean waitForData) {
    AttributeColumn<T> column = makeColumn(attribute, isNumeric);
    if (waitForData) {
      column.startWaitingForData();
    }
    addAttributeColumn(column, attribute);
  }

  protected void addColumn(Column<T, ?> column, String title) {
    TextHeader header = new TextHeader(title);
    header.setHeaderStyleNames("lightBorderLeft");
    cellTable().addColumn(column, header);
  }

  protected void addAttributeColumn(AttributeColumn<T> column, Attribute attribute) {
    attributeColumns.put(attribute, column);
    addColumn(column, attribute.title());
  }

  protected void addNonAttributeColumn(Column<T, ?> column, String title) {
    nonAttributeColumns.add(column);
    addColumn(column, title);
  }

  public void setupTable(T[] entries, MatchCondition condition) {
    setConditionAttributes(condition);

    for (Attribute attribute : classAttributes()) {
      addNewColumn(attribute, false, false);
    }

    addAdhocColumns();

    for (Attribute attribute : conditionAttributes) {
      addNewColumn(attribute, true, false);
    }

    cellTable().setRowData(Arrays.asList(entries));

    delegate.finishedSettingUpTable();
  }

  protected void addAdhocColumns() {
    for (Attribute attribute : adhocAttributes()) {
      addAttributeColumn(makeColumn(attribute, false), attribute);
    }
  }

  public void removeAttributeColumn(Attribute attribute) {
    Column<T, ?> column = attributeColumns.get(attribute);
    cellTable().removeColumn(column);
    attributeColumns.remove(attribute);
  }

  public void gotDataForAttribute(Attribute attribute) {
    attributeColumns.get(attribute).stopWaitingForData();
  }

  public void clear() {
    for (Attribute attribute : attributeColumns.keySet()) {
      cellTable().removeColumn(attributeColumns.get(attribute));
    }
    attributeColumns.clear();
    for (Column<T, ?> column : nonAttributeColumns) {
      cellTable().removeColumn(column);
    }
    nonAttributeColumns.clear();
    conditionAttributes.clear();
    additionalAttributes.clear();
  }

  /**
   * A column for displaying an attribute value for a sample or unit.
   */
  protected abstract class AttributeColumn<S> extends TooltipColumn<S> {
    protected Attribute attribute;
    protected boolean isNumeric;

    private boolean waitingForData = false;

    public void startWaitingForData() {
      waitingForData = true;
    }
    public void stopWaitingForData() {
      waitingForData = false;
    }

    public AttributeColumn(Cell<String> cell, Attribute attrib, boolean numeric) {
      super(cell);
      attribute = attrib;
      isNumeric = numeric;
      setCellStyleNames("lightBorderLeft");
    }

    public Attribute attribute() {
      return attribute;
    }

    protected abstract String getData(S s);

    @Override
    public String getValue(S s) {
      if (waitingForData) {
        return "Waiting for data...";
      } else {
        String value = getData(s);
        if (isNumeric && value != null) {
          try {
            return Utils.formatNumber(Double.parseDouble(value.replace(",", "")));
          } catch (NumberFormatException e) {
            return "Invalid number: " + value;
          }
        } else {
          return value;
        }
      }
    }

    @Override
    public String getTooltip(S s) {
      return getValue(s);
    }
  }
}

