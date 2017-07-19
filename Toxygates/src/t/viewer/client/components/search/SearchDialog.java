package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

/**
 * Sample search interface that allows the user to edit search conditions,
 * trigger a search, and display the results.
 */
public class SearchDialog extends Composite implements SearchDelegate, ResultTableDelegate {
  private DataListenerWidget widget;
  private AppInfo appInfo;
  private SampleServiceAsync sampleService;

  private ConditionEditor conditionEditor;

  private Button downloadButton;
  private Label resultCountLabel;
  private DialogBox waitDialog;

  private ResultTable<Sample> sampleTableHelper = new SampleTable(this);
  private ResultTable<Unit> unitTableHelper = new UnitTable(this);

  private SampleSearch sampleSearch;
  private UnitSearch unitSearch;
  private Search<?> currentSearch = null;

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
  
  public SearchDialog(DataListenerWidget widget, AppInfo appInfo, SampleServiceAsync sampleService,
      SampleClass sampleClass) {
    this.widget = widget;
    this.appInfo = appInfo;
    this.sampleService = sampleService;
    
    sampleSearch = new SampleSearch(this, sampleTableHelper, sampleService, sampleClass);
    unitSearch = new UnitSearch(this, unitTableHelper, sampleService, sampleClass);

    ScrollPanel searchPanel = new ScrollPanel();
    searchPanel.setSize("800px", "800px");    
    conditionEditor = new ConditionEditor(sampleParameters());
    
    Button sampleSearchButton = new Button("Sample Search");
    sampleSearchButton.addClickHandler(new ClickHandler() {      
      @Override
      public void onClick(ClickEvent event) {
        sampleSearch.attemptSearch(conditionEditor.getCondition());
      }
    });

    Button unitSearchButton = new Button("Unit Search");
    unitSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        unitSearch.attemptSearch(conditionEditor.getCondition());
      }
    });

    resultCountLabel = new Label();

    downloadButton = new Button("Download CSV...", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        SearchDialog.this.sampleService.prepareUnitCSVDownload(unitSearch.searchResult,
            unitTableHelper.allKeys(),
            new PendingAsyncCallback<String>(SearchDialog.this.widget,
                "Unable to prepare the data for download,") {
              @Override
              public void handleSuccess(String url) {
                Utils.displayURL("Your download is ready.", "Download", url);
              }
            });
      }
    });
    downloadButton.setVisible(false);

    HorizontalPanel tools =
        Utils.mkHorizontalPanel(true, unitSearchButton, sampleSearchButton, resultCountLabel,
            downloadButton);
    VerticalPanel vp = Utils.mkVerticalPanel(true, conditionEditor, tools, unitTableHelper.table,
        sampleTableHelper.table);
    searchPanel.add(vp);

    initWidget(searchPanel);    
  }
  
  protected void hideTables() {
    sampleTableHelper.clear();
    unitTableHelper.clear();
  }

  /*
   * SearchDelegate methods
   */
  @Override
  public void searchStarted(Search<?> search) {
    if (waitDialog == null) {
      waitDialog = Utils.waitDialog();
    } else {
      waitDialog.show();
    }

    resultCountLabel.setText("");
  }

  @Override
  public void searchEnded(Search<?> search, int numResults) {
    waitDialog.hide();
    hideTables();
    resultCountLabel.setText("Found " + numResults + " results");
    currentSearch = search;
    downloadButton.setVisible((currentSearch == unitSearch));
  }
}
