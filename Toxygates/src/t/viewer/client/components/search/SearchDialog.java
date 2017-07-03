package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.SampleClass;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

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
//    java.util.Collections.sort(r);
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
    private List<TextColumn<T>> columns = new LinkedList<TextColumn<T>>();

    protected abstract List<String> getKeys(T[] entries, MatchCondition cond);
    protected abstract TextColumn<T> makeColumn(String key);
    protected abstract void trackAnalytics();
    protected abstract void asyncSearch(SampleClass sc, MatchCondition cond,
        AsyncCallback<T[]> callback);

    protected void addColumn(String key) {
      TextColumn<T> column = makeColumn(key);
      columns.add(column);
      table.addColumn(column, key);
    }

    public void setupTable(T[] entries, MatchCondition cond) {
      List<String> keys = getKeys(entries, cond);

      for (String key : keys) {
        addColumn(key);
      }

      table.setRowData(Arrays.asList(entries));
    }

    public void clear() {
      for (TextColumn<T> column : columns) {
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

    protected abstract class KeyColumn<S> extends TextColumn<S> {
      protected String keyName;

      public KeyColumn<S> init(String key) {
        keyName = key;
        return this;
      }
    }
  }

  private class UnitTableHelper extends TableHelper<Unit> {
    @Override
    public List<String> getKeys(Unit[] entries, MatchCondition cond) {
      return new ArrayList<String>(entries[0].keys());
    }

    @Override
    public TextColumn<Unit> makeColumn(String key) {
      return new KeyColumn<Unit>() {
        @Override
        public String getValue(Unit unit) {
          return unit.get(keyName);
        }
      }.init(key);
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
    final String[] keys = { "compound_name", "exposure_time", "sample_id" };
    
    @Override
    public List<String> getKeys(Sample[] entries, MatchCondition condition) {
      List<String> r = new ArrayList<String>(Arrays.asList(keys));
      for (BioParamValue bp: condition.neededParameters()) {
        r.add(bp.id());
      }      
      return r;
    }

    @Override
    public TextColumn<Sample> makeColumn(String key) {
      return new KeyColumn<Sample>() {
        @Override
        public String getValue(Sample sample) {
          return sample.get(keyName);
        }
      }.init(key);
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
