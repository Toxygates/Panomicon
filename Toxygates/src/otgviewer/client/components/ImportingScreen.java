package otgviewer.client.components;

import java.util.List;

import t.common.shared.sample.Group;

public interface ImportingScreen extends Screen {
  boolean importProbes(String[] probes);

  boolean importColumns(List<Group> groups);
}
