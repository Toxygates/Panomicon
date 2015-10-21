package t.admin.client;

import java.util.Date;

import javax.annotation.Nullable;

import t.admin.shared.Platform;

import com.google.gwt.user.client.ui.TextArea;

public class PlatformEditor extends ManagedItemEditor {

  private @Nullable PlatformUploader uploader;
  protected TextArea publicComments;

  public PlatformEditor(@Nullable Platform p, boolean addNew) {
    super(p, addNew);
    publicComments = addTextArea("Public comments");

    if (p != null) {
      publicComments.setValue(p.getPublicComment());
    }

    if (addNew) {
      uploader = new PlatformUploader();
      vp.add(uploader);
    }
    addCommands();
  }

  @Override
  protected void triggerEdit() {
    Platform p = new Platform(idText.getValue(), 0, commentArea.getValue(), 
        new Date(), publicComments.getValue());
    if (addNew) {
      maintenanceService.addPlatformAsync(p, uploader.affyRadio.getValue(), new TaskCallback(
          "Add platform") {
        @Override
        void onCompletion() {
          onFinish();
          onFinishOrAbort();
        }
      });

    } else {
      maintenanceService.update(p, editCallback());
    }
  }

}
