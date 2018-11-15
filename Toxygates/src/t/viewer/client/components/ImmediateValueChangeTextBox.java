package t.viewer.client.components;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Custom TextBox that fires ValueChangeEvents on text cut/paste, and after
 * every keypress (regular TextBoxes won't fire a ValueChangeEvent until after
 * losing input focus).
 */
public class ImmediateValueChangeTextBox extends TextBox {
  public ImmediateValueChangeTextBox() {
    super();
    addCutHandler(this.getElement());
    addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        ValueChangeEvent.fire(ImmediateValueChangeTextBox.this, getText());
      }
    });
  }

  private void fireDeferredValueChangeEvent() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        ValueChangeEvent.fire(ImmediateValueChangeTextBox.this, getText());
      }
    });
  }

  @SuppressWarnings("deprecation") // suppress deprecation of Element
  private native void addCutHandler(com.google.gwt.user.client.Element element) /*-{
    var self = this;
    element.oncut = element.onpaste = function(e) {
      self.@t.viewer.client.components.ImmediateValueChangeTextBox::fireDeferredValueChangeEvent()();
    }
  }-*/;
}