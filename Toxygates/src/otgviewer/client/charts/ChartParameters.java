package otgviewer.client.charts;

import java.util.LinkedList;
import java.util.List;

import otgviewer.client.components.Screen;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;

public class ChartParameters {
  public final Screen screen;
  public final List<Group> groups;
  public final ValueType vt;
  public final String title;

  public ChartParameters(Screen screen, ValueType vt, String title) {
    this.screen = screen;
    this.groups = new LinkedList<Group>();
    this.vt = vt;
    this.title = title;
  }

  public ChartParameters(Screen screen, List<Group> groups, ValueType vt, String title) {
    this.screen = screen;
    this.groups = groups;
    this.vt = vt;
    this.title = title;
  }
}
