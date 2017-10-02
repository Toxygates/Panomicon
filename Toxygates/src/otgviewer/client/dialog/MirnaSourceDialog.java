package otgviewer.client.dialog;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.rpc.ProbeServiceAsync;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

public class MirnaSourceDialog extends InteractionDialog {
  MirnaSourceSelector selector;
  ProbeServiceAsync probeService;
  
  VerticalPanel vp;
  public MirnaSourceDialog(DataListenerWidget parent, 
                           ProbeServiceAsync probeService,
                           MirnaSource[] availableSources) {
    super(parent);
    selector = new MirnaSourceSelector(availableSources);
    this.probeService = probeService;
    vp = Utils.mkVerticalPanel(true);
    vp.add(selector);
    
    Button okButton = new Button("OK", new ClickHandler() {
      @Override
      public void onClick(ClickEvent e) {
        userProceed();        
      }      
    });
    
    Button cancelButton = new Button("Cancel", new ClickHandler() {
      @Override
      public void onClick(ClickEvent e) {
        userCancel();        
      }      
    });
    
    Panel p = Utils.mkHorizontalPanel(true, okButton, cancelButton);
    vp.add(p);
  }
  
  @Override
  protected Widget content() {
    return vp;
  }
  
  @Override
  protected void userProceed() {
    probeService.setMirnaSources(selector.getSelections(), new PendingAsyncCallback<Void>(parent) {
      @Override
      public void handleSuccess(Void success) {
        MirnaSourceDialog.super.userProceed();
      }
    });
  }

}
