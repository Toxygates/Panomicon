package t.viewer.client.intermine;

import t.common.client.components.ItemSelector;
import t.viewer.shared.AppInfo;
import t.viewer.shared.intermine.IntermineInstance;

public class InstanceSelector extends ItemSelector<IntermineInstance> {

  private IntermineInstance[] instances;
  public InstanceSelector(AppInfo info) {
    super(info.intermineInstances());
    instances = info.intermineInstances();
  }
  
  @Override
  protected IntermineInstance[] values() {
    return instances;
  }

  @Override
  protected String titleForValue(IntermineInstance t) {
    return t.title();
  }

}
