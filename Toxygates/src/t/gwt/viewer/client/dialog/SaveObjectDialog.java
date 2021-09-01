package t.gwt.viewer.client.dialog;

import java.util.function.Consumer;

import com.google.gwt.user.client.Window;

import t.gwt.viewer.client.storage.NamedObjectStorage;
import t.gwt.viewer.client.storage.StorageProvider;

public class SaveObjectDialog extends InputDialog {
  private NamedObjectStorage<?> storage;
  
  private Consumer<String> saveAction;
  private CloseDialogAction closeAction;
  
  public interface CloseDialogAction {
    void run();
  }

  public SaveObjectDialog(String message, String initialText,
      NamedObjectStorage<?> storage,
      Consumer<String> saveAction, CloseDialogAction closeAction) {
    super(message, initialText);
    this.storage = storage;
    this.saveAction = saveAction;
    this.closeAction = closeAction;
    onTextBoxValueChange(initialText);
  }
  
  @Override
  protected void onChange(String value) {
    if (value != "") { // Empty string means OK button with blank text input
      if (value != null) { // value == null means cancel button
        if (value.trim().equals("")) {
          Window.alert("Please enter a group name.");
          return;
        }
        if (!StorageProvider.isAcceptableString(value, "Unacceptable group name.")) {
          Window.alert("Please enter a valid group name.");
          return;
        }
        saveAction.accept(value);
      }
      closeAction.run();
    }
  }
  
  @Override
  protected void onTextBoxValueChange(String newValue) {
    if (storage != null && storage.reservedName(newValue)) {
      submitButton.setText("Save new");
      submitButton.setEnabled(false);
    } else if (storage != null && storage.containsKey(newValue)) {
      submitButton.setText("Overwrite");
      submitButton.setEnabled(true);
    } else {
      submitButton.setText("Save new");
      submitButton.setEnabled(true);
    }
  }
}
