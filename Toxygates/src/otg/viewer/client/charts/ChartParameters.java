package otg.viewer.client.charts;

import java.util.LinkedList;
import java.util.List;

import otg.viewer.client.components.OTGScreen;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;

/**
 * Parameters for chart creation and display.
 */
public class ChartParameters {
  public final OTGScreen screen;
  public final List<Group> groups;
  public final ValueType vt;
  public final String title;

  public ChartParameters(OTGScreen screen, ValueType vt, String title) {
    this.screen = screen;
    this.groups = new LinkedList<Group>();
    this.vt = vt;
    this.title = title;
  }

  public ChartParameters(OTGScreen screen, List<Group> groups, ValueType vt, String title) {
    this.screen = screen;
    this.groups = groups;
    this.vt = vt;
    this.title = title;
  }
}
