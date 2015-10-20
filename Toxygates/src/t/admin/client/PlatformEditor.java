package t.admin.client;

import java.util.Date;

import javax.annotation.Nullable;

import t.admin.shared.Platform;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TextArea;

public class PlatformEditor extends ManagedItemEditor {

  private @Nullable PlatformUploader uploader;
  protected TextArea publicComments;

  public PlatformEditor(boolean addNew) {
    super(addNew);
    publicComments = addTextArea("Public comments");

    if (addNew) {
      uploader = new PlatformUploader();
      vp.add(uploader);
    }

    addCommands();
  }

  @Override
  protected void triggerEdit() {
    Platform p = new Platform(idText.getText(), 0, commentArea.getText(), new Date());
    if (addNew) {
      maintenanceService.addPlatformAsync(p, uploader.affyRadio.getValue(), new TaskCallback(
          "Add platform") {
        @Override
        void onCompletion() {
          // TODO
        }
      });

    } else {
      maintenanceService.update(p, new AsyncCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
          // TODO Auto-generated method stub

        }

        @Override
        public void onFailure(Throwable caught) {
          // TODO Auto-generated method stub

        }
      });
    }
  }

}
