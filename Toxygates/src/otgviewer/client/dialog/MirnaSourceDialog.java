package otgviewer.client.dialog;

import otgviewer.client.components.Screen;
import t.viewer.client.PersistedState;
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
  PersistedState<MirnaSource[]> state;
  Screen screen;
  VerticalPanel vp;
  
  public MirnaSourceDialog(Screen parent, 
                           ProbeServiceAsync probeService,
                           MirnaSource[] availableSources,
                           PersistedState<MirnaSource[]> state) {
    super(parent);
    
    this.selector = new MirnaSourceSelector(availableSources, state.getValue());
    this.state = state;
    this.screen = parent;
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
    state.changeAndPersist(screen.getParser(), 
      selector.getSelection().toArray(new MirnaSource[0]));
    MirnaSourceDialog.super.userProceed();    
  }
}
