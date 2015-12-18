package otgviewer.client.components;

import java.util.Arrays;

import t.common.shared.Dataset;
import t.common.shared.SampleClass;
import t.viewer.client.rpc.SparqlServiceAsync;
import t.viewer.shared.AppInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class FilterTools extends DataListenerWidget {
  private HorizontalPanel filterTools;
  private DataFilterEditor dfe;
  final Screen screen;
  final SparqlServiceAsync sparqlService;
  
  public FilterTools(final Screen screen) {
    AppInfo appInfo = screen.appInfo();
    chosenDatasets = appInfo.datasets();
    this.screen = screen;
    sparqlService = screen.sparqlService();
    
    filterTools = new HorizontalPanel();
    initWidget(filterTools);
    
    filterTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    filterTools.addStyleName("slightlySpacedLeftRight");

    Button b = new Button("Data...");
    filterTools.add(b);
    b.addClickHandler(new ClickHandler() {          
        @Override
        public void onClick(ClickEvent event) {
            showDatasetSelector();                                      
        }
    });
    
    dfe = new DataFilterEditor(screen) {
        @Override
        protected void changeSampleClass(SampleClass sc) {
            super.changeSampleClass(sc);                
            screen.sampleClassChanged(sc);
            
            //TODO Actions are enqueued in TimeDoseGrid and CompoundSelector.
            //I'm not sure that exposing the action queue mechanism 
            //like this is a good thing to do. Think of a better way.
            screen.runActions();
        }
    };
    this.addListener(dfe);
    filterTools.add(dfe);         
  }
  
  protected void showDatasetSelector() {
    final DialogBox db = new DialogBox(false, true);
    //TODO set init. selection
    DatasetSelector dsel = new DatasetSelector(Arrays.asList(screen.appInfo().datasets()), 
            Arrays.asList(chosenDatasets)) {
        @Override
        public void onOK() {                            
            datasetsChanged(selector.getSelection().toArray(new Dataset[0]));
            sparqlService.chooseDatasets(chosenDatasets,
                    new PendingAsyncCallback<Void>(screen, "Unable to choose datasets") {                  
                public void handleSuccess(Void v) {
                    dfe.update();
                }
            });             
            db.hide();              
        }
        
        @Override
        public void onCancel() {
            super.onCancel();
            db.hide();
        }
    };
    db.setText("Select datasets");
    db.setWidget(dsel);
    db.setWidth("500px");
    db.show();
}


}
