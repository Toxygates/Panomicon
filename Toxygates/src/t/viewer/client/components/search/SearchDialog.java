package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.table.TooltipColumn;
import t.viewer.shared.AppInfo;

/**
 * Sample search interface that allows the user to edit search conditions,
 * trigger a search, and display the results.
 */
public class SearchDialog extends Composite {
  private AppInfo appInfo;
  private ConditionEditor conditionEditor;
  private SampleServiceAsync sampleService;
  private SampleClass sampleClass;
  private TableHelper<Sample> sampleTableHelper = new SampleTableHelper();
  private TableHelper<Unit> unitTableHelper = new UnitTableHelper();

  private Label resultCountLabel;
  private DialogBox waitDialog;
  
  private static String DECIMAL_FORMAT = "#.000";

  /**
   * Available search parameters. BioParamValue is here used to identify
   * parameters in general, and not specific values.
   * @return
   */
  private Collection<BioParamValue> sampleParameters() {
    BioParamValue[] params = appInfo.bioParameters();
    List<BioParamValue> r = new ArrayList<BioParamValue>();
    for (BioParamValue bp : params) {
      if (bp instanceof NumericalBioParamValue) {
        r.add(bp);
      }
    }
    java.util.Collections.sort(r);
    return r;
  }
  
  public SearchDialog(AppInfo appInfo, SampleServiceAsync sampleService,
      SampleClass sampleClass) {
    this.appInfo = appInfo;
    this.sampleService = sampleService;
    this.sampleClass = sampleClass;
    
    ScrollPanel searchPanel = new ScrollPanel();
    searchPanel.setSize("800px", "800px");    
    conditionEditor = new ConditionEditor(sampleParameters());
    
    Button sampleSearchButton = new Button("Sample Search");
    sampleSearchButton.addClickHandler(new ClickHandler() {      
      @Override
      public void onClick(ClickEvent event) {
        sampleTableHelper.performSearch(conditionEditor.getCondition());
      }
    });

    Button unitSearchButton = new Button("Unit Search");
    unitSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        unitTableHelper.performSearch(conditionEditor.getCondition());
      }
    });

    resultCountLabel = new Label();

    HorizontalPanel tools =
        Utils.mkHorizontalPanel(true, unitSearchButton, sampleSearchButton, resultCountLabel);
    VerticalPanel vp = Utils.mkVerticalPanel(true, conditionEditor, tools, unitTableHelper.table,
        sampleTableHelper.table);
    searchPanel.add(vp);

    initWidget(searchPanel);    
  }
  
  protected void hideTables() {
    sampleTableHelper.clear();
    unitTableHelper.clear();
  }

  private abstract class TableHelper<T> {
    private CellTable<T> table = new CellTable<T>();
    private List<Column<T, String>> columns = new LinkedList<Column<T, String>>();

    protected abstract Column<T, String> makeBasicColumn(String key);
    protected abstract Column<T, String> makeNumericColumn(String key);
    protected abstract void trackAnalytics();
    protected abstract void asyncSearch(SampleClass sc, MatchCondition cond,
        AsyncCallback<T[]> callback);

    //Could also have macro parameters here, such as organism, tissue etc
    //but currently the search is always constrained on those parameters
    private final String[] classKeys = {"compound_name", "dose_level", "exposure_time"};
    private final String[] adhocKeys = {"sample_id"};

    protected String[] getClassKeys() {
      return classKeys;
    }

    protected String[] getAdhocKeys() {
      return adhocKeys;
    }
    
    private List<String> getKeys(MatchCondition condition) {
      List<String> r = new ArrayList<String>();
      for (BioParamValue bp: condition.neededParameters()) {
        r.add(bp.id());
      }      
      return r;
    }

    protected void addColumn(Column<T, String> column, String title) {
      columns.add(column);
      table.addColumn(column, title);
    }

    public void setupTable(T[] entries, MatchCondition cond) {
      for (String key : getClassKeys()) {
        addColumn(makeBasicColumn(key), key);
      }
      
      addAdhocColumns();

      for (String key : getKeys(cond)) {
        addColumn(makeNumericColumn(key), key);
      }

      table.setRowData(Arrays.asList(entries));
    }

    protected void addAdhocColumns() {
      for (String key : getAdhocKeys()) {
        addColumn(makeBasicColumn(key), key);
      }
    }

    public void clear() {
      for (Column<T, String> column : columns) {
        table.removeColumn(column);
      }
      columns.clear();
    }

    protected String searchEntityName() {
      return "entities";
    }

    public void performSearch(final @Nullable MatchCondition condition) {
      if (condition == null) {
        Window.alert("Please define the search condition.");
        return;
      }

      if (waitDialog == null) {
        waitDialog = Utils.waitDialog();
      } else {
        waitDialog.show();
      }

      trackAnalytics();

      resultCountLabel.setText("");

      asyncSearch(sampleClass, condition, new AsyncCallback<T[]>() {

        @Override
        public void onSuccess(T[] result) {
          waitDialog.hide();
          hideTables();
          setupTable(result, condition);
          resultCountLabel.setText("Found " + result.length + " " + searchEntityName());
        }

        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Failure: " + caught);
        }
      });
    }

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
          return NumberFormat.getFormat(DECIMAL_FORMAT).format(Double.parseDouble(string));
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

  private class UnitTableHelper extends TableHelper<Unit> {
    private TextCell textCell = new TextCell();

    protected class UnitKeyColumn extends KeyColumn<Unit> {
      public UnitKeyColumn(Cell<String> cell, String key) {
        super(cell, key);
      }

      @Override
      public String getData(Unit unit) {
        return unit.get(keyName);
      }
    }

    public UnitKeyColumn makeColumn(String key) {
      return new UnitKeyColumn(textCell, key);
    }

    @Override
    public TooltipColumn<Unit> makeBasicColumn(String key) {
      return makeColumn(key);
    }

    @Override
    public TooltipColumn<Unit> makeNumericColumn(String key) {
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

    @Override
    protected void trackAnalytics() {
      Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_UNIT_SEARCH);
    }

    @Override
    protected void asyncSearch(SampleClass sampleClass, MatchCondition condition,
        AsyncCallback<Unit[]> callback) {
      sampleService.unitSearch(sampleClass, condition, callback);
    }

    @Override
    protected String searchEntityName() {
      return "units";
    }
  }

  private class SampleTableHelper extends TableHelper<Sample> {
    private TextCell textCell = new TextCell();

    private TooltipColumn<Sample> makeColumn(String key) {
      return new KeyColumn<Sample>(textCell, key) {
        @Override
        public String getData(Sample sample) {
          return sample.get(keyName);
        }
      };
    }

    @Override
    public TooltipColumn<Sample> makeBasicColumn(String key) {
      return makeColumn(key);
    }

    @Override
    public TooltipColumn<Sample> makeNumericColumn(String key) {
      return makeColumn(key);
    }

    @Override
    protected void trackAnalytics() {
      Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_SAMPLE_SEARCH);
    }

    @Override
    protected void asyncSearch(SampleClass sampleClass, MatchCondition condition,
        AsyncCallback<Sample[]> callback) {
      sampleService.sampleSearch(sampleClass, condition, callback);
    }

    @Override
    protected String searchEntityName() {
      return "samples";
    }
  }
}
