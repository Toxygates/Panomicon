package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.user.cellview.client.CellTable;

import t.common.client.Utils;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.search.MatchCondition;
import t.viewer.client.table.TooltipColumn;

/**
 * Manages a table for displaying the results of a sample/unit search.
 */
public abstract class ResultTable<T> {
  public interface Delegate {
    void finishedSettingUpTable();
  }

  protected CellTable<T> table = new CellTable<T>();
  private Map<String, KeyColumn<T>> columns = new HashMap<String, KeyColumn<T>>();
  private List<String> additionalKeys = new LinkedList<String>();
  private List<String> conditionKeys = new ArrayList<String>();
  private Delegate delegate; // we'll need this in the future

  protected abstract KeyColumn<T> makeColumn(String key, boolean numeric);

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
    keys.addAll(Arrays.asList(requiredKeys()));
    keys.addAll(Arrays.asList(nonRequiredKeys()));

    return keys.toArray(new String[0]);
  }

  /**
   * Keys that cannot be hidden
   */
  public String[] requiredKeys() {
    List<String> keys = new ArrayList<String>();
    keys.addAll(Arrays.asList(classKeys()));
    keys.addAll(Arrays.asList(adhocKeys()));
    keys.addAll(conditionKeys);

    return keys.toArray(new String[0]);
  }

  /**
   * Keys that can be hidden
   */
  public String[] nonRequiredKeys() {
    return additionalKeys.toArray(new String[0]);
  }

  /**
   * Keys that identify the sample class
   */
  protected String[] classKeys() {
    return classKeys;
  }

  /**
   * Other required keys
   */
  protected String[] adhocKeys() {
    return adhocKeys;
  }

  private void setConditionKeys(MatchCondition condition) {
    assert(conditionKeys.size() == 0);  
    for (BioParamValue bp : condition.neededParameters()) {
      conditionKeys.add(bp.id()); 
    }
  }

  public void addExtraColumn(String key, boolean isNumeric, boolean waitForData) {
    addNewColumn(key, isNumeric, waitForData);
    additionalKeys.add(key);
  }

  private void addNewColumn(String key, boolean isNumeric, boolean waitForData) {
    KeyColumn<T> column = makeColumn(key, isNumeric);
    if (waitForData) {
      column.startWaitingForData();
    }
    addColumn(column, key);
  }

  protected void addColumn(KeyColumn<T> column, String title) {
    columns.put(title, column);
    table.addColumn(column, title);
  }

  public void setupTable(T[] entries, MatchCondition condition) {
    setConditionKeys(condition);

    for (String key : classKeys()) {
      addNewColumn(key, false, false);
    }

    addAdhocColumns();

    for (String key : conditionKeys) {
      addNewColumn(key, true, false);
    }

    table.setRowData(Arrays.asList(entries));

    delegate.finishedSettingUpTable();
  }

  protected void addAdhocColumns() {
    for (String key : adhocKeys()) {
      addColumn(makeColumn(key, false), key);
    }
  }

  public void removeColumn(String key) {
    KeyColumn<T> column = columns.get(key);
    table.removeColumn(column);
    columns.remove(key);
  }

  public void gotDataForKey(String key) {
    columns.get(key).stopWaitingForData();
  }

  public void clear() {
    for (String key : columns.keySet()) {
      table.removeColumn(columns.get(key));
    }
    columns.clear();
    conditionKeys.clear();
    additionalKeys.clear();
  }

  /**
   * A column for displaying an attribute value for a sample or unit.
   */
  protected abstract class KeyColumn<S> extends TooltipColumn<S> {
    protected String keyName;
    protected boolean isNumeric;

    private boolean waitingForData = false;

    public void startWaitingForData() {
      waitingForData = true;
    }
    public void stopWaitingForData() {
      waitingForData = false;
    }

    public KeyColumn(Cell<String> cell, String key, boolean numeric) {
      super(cell);
      keyName = key;
      isNumeric = numeric;
    }

    public String key() {
      return keyName;
    }

    protected abstract String getData(S s);

    @Override
    public String getValue(S s) {
      if (waitingForData) {
        return "Waiting for data...";
      } else {
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
    }

    @Override
    public String getTooltip(S s) {
      return getData(s);
    }
  }
}

