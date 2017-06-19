package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.SampleClass;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.search.MatchCondition;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
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
  
  private Collection<String> sampleParameters() {
    BioParamValue[] params = appInfo.bioParameters();
    List<String> r = new ArrayList<String>();
    for (BioParamValue bp: params) {
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
    
    Button searchButton = new Button("Search");
    searchButton.addClickHandler(new ClickHandler() {      
      @Override
      public void onClick(ClickEvent event) {
       performSearch(conditionEditor.getCondition()); 
      }
    });
    HorizontalPanel tools = Utils.mkHorizontalPanel(true, searchButton);    
    VerticalPanel vp = Utils.mkVerticalPanel(true, conditionEditor, tools);
    searchPanel.add(vp);
    
    initWidget(searchPanel);    
  }
  
  private void performSearch(@Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }
    sampleService.sampleSearch(sampleClass, condition,  
        new AsyncCallback<Void>() {
          
          @Override
          public void onSuccess(Void result) {
            
          }
          
          @Override
          public void onFailure(Throwable caught) {
                 
          }
        });
  }
  
}
