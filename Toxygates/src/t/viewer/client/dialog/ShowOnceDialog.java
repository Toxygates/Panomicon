package t.viewer.client.dialog;

import com.google.gwt.user.client.ui.*;

import t.viewer.client.Utils;

/**
 * A message dialog that lets the user suppress future occurrences.
 */
public class ShowOnceDialog {
  boolean shouldShow = true;

  DialogBox db = new DialogBox();
  VerticalPanel vp = Utils.mkVerticalPanel(true);
  Label message = new Label();
  CheckBox check = new CheckBox("Do not show this message again");
  Button b = new Button("Close");

  public ShowOnceDialog() {
    vp.add(message);
    message.setWordWrap(true);
    message.addStyleName("popupMessage");
    vp.add(check);
    vp.add(b);
    db.add(vp);
    b.addClickHandler(e -> close());
  }

  public void showIfDesired(String message) {
    if (shouldShow) {
      this.message.setText(message);
      Utils.displayInCenter(db);
    }
  }

  public void close() {
    this.shouldShow = !check.getValue();
    db.hide();
  }
}
