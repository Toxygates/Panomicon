package otgviewer.client;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

import otgviewer.client.components.DLWScreen;
import otgviewer.client.components.ScreenManager;
import t.model.SampleClass;

/**
 * A screen that supports FilterTools. More code may be moved here from the various sub-screens
 * in the future.
 */
abstract public class DataFilterScreen extends DLWScreen {
    
  public DataFilterScreen(String title, String key, boolean showGroups, ScreenManager man,
      TextResource helpHTML, ImageResource helpImage) {
    super(title, key, showGroups, man, helpHTML, helpImage);
  }
  @Override
  public void changeSampleClass(SampleClass sc) {
    // On this screen, ignore the blank sample class set by
    // DataListenerWidget
    if (!sc.getMap().isEmpty()) {
      super.changeSampleClass(sc);
      storeSampleClass(getParser());
    }
  }

  private boolean initialised = false;
  @Override 
  public void show() {
    super.show();
    if (!initialised) {
      //Force reloading of sample classes
      changeDatasets(chosenDatasets);
      initialised = true;
    }
  }
}
