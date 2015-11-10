package otgviewer.client.components;

import java.util.List;

public interface SaveActionHandler {

  /*
   * Override this function to handle post save event and what genes were saved
   */
  void onSaved(String title, List<String> items);

  /*
   * Override this function to handle what genes were saved
   */
  void onCanceled();

}
