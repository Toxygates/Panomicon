package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

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

import t.common.shared.SampleClass;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SampleServiceAsync;
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
  private CellTable<Unit> unitTable = new CellTable<Unit>();
  private List<TextColumn<Unit>> columns = new LinkedList<TextColumn<Unit>>();
  private DialogBox waitDialog;
  
  private Collection<String> sampleParameters() {
    BioParamValue[] params = appInfo.bioParameters();
    List<String> r = new ArrayList<String>();
    for (BioParamValue bp : params) {
      if (bp instanceof NumericalBioParamValue) {
        r.add(bp.label());
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
        performSearch(conditionEditor.getCondition());
      }
    });

    Button unitSearchButton = new Button("Unit Search");
    unitSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        performUnitSearch(conditionEditor.getCondition());
      }
    });

    HorizontalPanel tools = Utils.mkHorizontalPanel(true, searchButton, unitSearchButton);
    VerticalPanel vp = Utils.mkVerticalPanel(true, conditionEditor, tools, unitTable);
    searchPanel.add(vp);

    initWidget(searchPanel);    
  }
  
  private void setUnitTableData(Unit[] units) {
    Set<String> unitKeys = units[0].keys();

    for (TextColumn<Unit> column : columns) {
      unitTable.removeColumn(column);
    }
    columns.clear();

    for (int i = 0; i < unitTable.getColumnCount(); i++) {
      unitTable.removeColumn(0);
    }

    for (String key : unitKeys) {
      TextColumn<Unit> column = new TextColumn<Unit>() {
        private String keyName;
        public String getValue(Unit unit) {
          return unit.get(keyName);
        }
        public TextColumn<Unit> init (String key) {
          keyName = key;
          return this;
        }
      }.init(key);
      columns.add(column);
      unitTable.addColumn(column, key);
    }

    unitTable.setRowData(Arrays.asList(units));
  }

  private void performSearch(@Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }
    sampleService.sampleSearch(sampleClass, condition, new AsyncCallback<Void>() {

      @Override
      public void onSuccess(Void result) {

      }

      @Override
      public void onFailure(Throwable caught) {

      }
    });
  }
  
  private void performUnitSearch(@Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }

    if (waitDialog == null) {
      waitDialog = Utils.waitDialog();
    } else {
      waitDialog.show();
    }

    sampleService.unitSearch(sampleClass, condition, new AsyncCallback<Unit[]>() {

      @Override
      public void onSuccess(Unit[] result) {
        waitDialog.hide();
        setUnitTableData(result);
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failure: " + caught);
      }
    });
  }

}
