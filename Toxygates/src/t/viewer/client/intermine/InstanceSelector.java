/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.intermine;

import t.common.client.components.ItemSelector;
import t.viewer.shared.AppInfo;
import t.viewer.shared.intermine.IntermineInstance;

public class InstanceSelector extends ItemSelector<IntermineInstance> {

  private IntermineInstance[] instances = new IntermineInstance[0];

  public InstanceSelector(AppInfo info) {
    super(false);
    instances = info.intermineInstances();
    updateListBoxChoices();
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
