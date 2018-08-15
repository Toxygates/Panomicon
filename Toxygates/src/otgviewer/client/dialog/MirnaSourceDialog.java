package otgviewer.client.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.Screen;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.rpc.ProbeServiceAsync;
import t.viewer.shared.mirna.MirnaSource;

public class MirnaSourceDialog extends InteractionDialog {
  MirnaSourceSelector selector;
  ProbeServiceAsync probeService;
  VerticalPanel vp;
  Delegate delegate;
  
  public interface Delegate {
    void mirnaSourceDialogMirnaSourcesChanged(MirnaSource[] mirnaSources);
  }

  public MirnaSourceDialog(Screen parent, Delegate delegate,
                           ProbeServiceAsync probeService,
                           MirnaSource[] availableSources,
                           MirnaSource[] value) {
    super(parent);
    this.delegate = delegate;
    this.selector = new MirnaSourceSelector(availableSources, value);
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
    delegate.mirnaSourceDialogMirnaSourcesChanged(selector.getSelection().toArray(new MirnaSource[0]));
    MirnaSourceDialog.super.userProceed();    
  }
}
