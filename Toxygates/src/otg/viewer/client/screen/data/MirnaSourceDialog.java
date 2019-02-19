package otg.viewer.client.screen.data;

import java.util.Collection;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.OTGScreen;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.shared.mirna.MirnaSource;

public class MirnaSourceDialog extends InteractionDialog {
  MirnaSourceSelector selector;
  VerticalPanel vp;
  Delegate delegate;
  
  public interface Delegate {
    void mirnaSourceDialogMirnaSourcesChanged(MirnaSource[] mirnaSources);
  }

  public MirnaSourceDialog(OTGScreen parent, Delegate delegate,
                           MirnaSource[] availableSources,
                           List<MirnaSource> value) {
    super(parent);
    this.delegate = delegate;
    this.selector = new MirnaSourceSelector(availableSources, value);
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
    try {
      Collection<MirnaSource> selection = selector.getSelection();

      delegate.mirnaSourceDialogMirnaSourcesChanged(selection.toArray(new MirnaSource[0]));
      MirnaSourceDialog.super.userProceed();
    } catch (NumberFormatException e) {
      Window.alert("Please enter a numerical value as the score cutoff.");
    }
  }
}
