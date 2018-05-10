package otgviewer.client.components;

import otgviewer.client.SampleDetailScreen;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.viewer.client.StorageParser;

public class ScreenUtils {
  public static void displaySampleDetail(Screen screen, Sample b) {
    StorageParser p = screen.manager().getParser();
    Group g = new Group(screen.manager().schema(), "custom", new Sample[] {b});
    DataListenerWidget.storeCustomColumn(p, g);
    screen.manager().attemptProceed(SampleDetailScreen.key);
  }

}
