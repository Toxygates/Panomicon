package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;

import t.common.client.Utils;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.viewer.client.table.TooltipColumn;

interface ResultTableDelegate {

}

/**
 * Manages a table for displaying the results of a sample/unit search.
 */
abstract class ResultTable<T> {
  protected CellTable<T> table = new CellTable<T>();
  private List<Column<T, String>> columns = new LinkedList<Column<T, String>>();
  private List<String> conditionKeys;
  private ResultTableDelegate delegate; // we'll need this in the future

  protected abstract Column<T, String> makeBasicColumn(String key);
  protected abstract Column<T, String> makeNumericColumn(String key);

  // Could also have macro parameters here, such as organism, tissue etc
  // but currently the search is always constrained on those parameters
  private final String[] classKeys = {"compound_name", "dose_level", "exposure_time"};
  private final String[] adhocKeys = {"sample_id"};

  public ResultTable(ResultTableDelegate delegate) {
    this.delegate = delegate;
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
      addColumn(makeBasicColumn(key), key);
    }

    addAdhocColumns();

    for (String key : conditionKeys) {
      addColumn(makeNumericColumn(key), key);
    }

    table.setRowData(Arrays.asList(entries));
  }

  protected void addAdhocColumns() {
    for (String key : adhocKeys()) {
      addColumn(makeBasicColumn(key), key);
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

    public KeyColumn(Cell<String> cell, String key) {
      super(cell);
      keyName = key;
    }

    protected abstract String getData(S s);

    @Override
    public String getValue(S s) {
      String string = getData(s);
      try {
        return Utils.formatNumber(Double.parseDouble(string));
      } catch (NumberFormatException e) {
        return string;
      }
    }

    @Override
    public String getTooltip(S s) {
      return getData(s);
    }
  }
}

class UnitTable extends ResultTable<Unit> {
  private TextCell textCell = new TextCell();

  public UnitTable(ResultTableDelegate delegate) {
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

class SampleTable extends ResultTable<Sample> {
  private TextCell textCell = new TextCell();

  public SampleTable(ResultTableDelegate delegate) {
    super(delegate);
  }

  private TooltipColumn<Sample> makeColumn(String key) {
    return new KeyColumn<Sample>(textCell, key) {
      @Override
      public String getData(Sample sample) {
        return sample.get(keyName);
      }
    };
  }

  @Override
  protected TooltipColumn<Sample> makeBasicColumn(String key) {
    return makeColumn(key);
  }

  @Override
  protected TooltipColumn<Sample> makeNumericColumn(String key) {
    return makeColumn(key);
  }
}
