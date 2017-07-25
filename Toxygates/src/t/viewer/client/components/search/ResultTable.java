package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;

import t.common.client.Utils;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.search.MatchCondition;
import t.viewer.client.table.TooltipColumn;

/**
 * Manages a table for displaying the results of a sample/unit search.
 */
public abstract class ResultTable<T> {
  public interface Delegate {

  }

  protected CellTable<T> table = new CellTable<T>();
  private List<Column<T, String>> columns = new LinkedList<Column<T, String>>();
  private List<String> conditionKeys;
  private Delegate delegate; // we'll need this in the future

  protected abstract Column<T, String> makeColumn(String key, boolean numeric);

  // Could also have macro parameters here, such as organism, tissue etc
  // but currently the search is always constrained on those parameters
  private final String[] classKeys = {"compound_name", "dose_level", "exposure_time"};
  private final String[] adhocKeys = {"sample_id"};

  public ResultTable(Delegate delegate) {
    this.delegate = delegate;
  }

  public CellTable<T> table() {
    return table;
  }

  public String[] allKeys() {
    List<String> keys = new ArrayList<String>();
    keys.addAll(Arrays.asList(classKeys()));
    keys.addAll(Arrays.asList(adhocKeys()));
    keys.addAll(conditionKeys);

    return keys.toArray(new String[0]);
  }

  protected String[] classKeys() {
    return classKeys;
  }

  protected String[] adhocKeys() {
    return adhocKeys;
  }

  private void setConditionKeys(MatchCondition condition) {
    List<String> r = new ArrayList<String>();
    for (BioParamValue bp : condition.neededParameters()) {
      r.add(bp.id());
    }
    conditionKeys = r;
  }

  protected void addColumn(Column<T, String> column, String title) {
    columns.add(column);
    table.addColumn(column, title);
  }

  public void setupTable(T[] entries, MatchCondition condition) {
    setConditionKeys(condition);

    for (String key : classKeys()) {
      addColumn(makeColumn(key, false), key);
    }

    addAdhocColumns();

    for (String key : conditionKeys) {
      addColumn(makeColumn(key, true), key);
    }

    table.setRowData(Arrays.asList(entries));
  }

  protected void addAdhocColumns() {
    for (String key : adhocKeys()) {
      addColumn(makeColumn(key, false), key);
    }
  }

  public void clear() {
    for (Column<T, String> column : columns) {
      table.removeColumn(column);
    }
    columns.clear();
  }

  /**
   * A column for displaying an attribute value for a sample or unit.
   */
  protected abstract class KeyColumn<S> extends TooltipColumn<S> {
    protected String keyName;
    protected boolean isNumeric;

    public KeyColumn(Cell<String> cell, String key, boolean numeric) {
      super(cell);
      keyName = key;
      isNumeric = numeric;
    }

    protected abstract String getData(S s);

    @Override
    public String getValue(S s) {
      String string = getData(s);
      if (isNumeric) {
        try {
          return Utils.formatNumber(Double.parseDouble(string));
        } catch (NumberFormatException e) {
          return "Malformed number: " + string;
        }
      } else {
        return string;
      }
    }

    @Override
    public String getTooltip(S s) {
      return getData(s);
    }
  }
}

