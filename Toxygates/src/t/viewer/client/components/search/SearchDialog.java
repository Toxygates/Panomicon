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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import t.common.shared.SampleClass;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
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
    
    Button searchButton = new Button("Sample Search");
    searchButton.addClickHandler(new ClickHandler() {      
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

    HorizontalPanel tools = Utils.mkHorizontalPanel(true, searchButton, unitSearchButton);
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
    private List<TooltipColumn<T>> columns = new LinkedList<TooltipColumn<T>>();

    protected abstract TooltipColumn<T> makeColumn(String key, Cell<String> cell);
    protected abstract void trackAnalytics();
    protected abstract void asyncSearch(SampleClass sc, MatchCondition cond,
        AsyncCallback<T[]> callback);

    //Could also have macro parameters here, such as organism, tissue etc
    //but currently the search is always constrained on those parameters
    final String[] keys = { "compound_name", "exposure_time", "sample_id",
        "individual_id" };
    
    public List<String> getKeys(MatchCondition condition) {
      List<String> r = new ArrayList<String>(Arrays.asList(keys));
      for (BioParamValue bp: condition.neededParameters()) {
        r.add(bp.id());
      }      
      return r;
    }
    
    protected void addColumn(String key, Cell<String> cell) {
      TooltipColumn<T> column = makeColumn(key, cell);
      columns.add(column);
      table.addColumn(column, key);
    }

    public void setupTable(T[] entries, MatchCondition cond) {
      List<String> keys = getKeys(cond);
      TextCell textCell = new TextCell();

      for (String key : keys) {
        addColumn(key, textCell);
      }

      table.setRowData(Arrays.asList(entries));
    }

    public void clear() {
      for (TooltipColumn<T> column : columns) {
        table.removeColumn(column);
      }
      columns.clear();
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

      asyncSearch(sampleClass, condition, new AsyncCallback<T[]>() {

        @Override
        public void onSuccess(T[] result) {
          waitDialog.hide();
          hideTables();
          setupTable(result, condition);
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
    @Override
    public TooltipColumn<Unit> makeColumn(String key, Cell<String> cell) {
      return new KeyColumn<Unit>(cell, key) {
        @Override
        public String getData(Unit unit) {
          return unit.get(keyName);
        }
      };
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
  }

  private class SampleTableHelper extends TableHelper<Sample> {
    @Override
    public TooltipColumn<Sample> makeColumn(String key, Cell<String> cell) {
      return new KeyColumn<Sample>(cell, key) {
        @Override
        public String getData(Sample sample) {
          return sample.get(keyName);
        }
      };
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
  }
}
