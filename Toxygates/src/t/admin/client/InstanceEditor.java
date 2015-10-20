/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.admin.client;

import java.util.Date;

import t.admin.shared.AccessPolicy;
import t.admin.shared.Instance;
import t.common.client.components.EnumSelector;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class InstanceEditor extends ManagedItemEditor {

  private final TextBox roleText;
  private final EnumSelector<AccessPolicy> policySelector;

  public InstanceEditor(boolean addNew) {
    super(addNew);

    Label l = new Label("Access policy");
    vp.add(l);

    policySelector = new EnumSelector<AccessPolicy>() {
      @Override
      protected AccessPolicy[] values() {
        return AccessPolicy.values();
      }

    };
    policySelector.listBox().setEnabled(addNew);
    vp.add(policySelector);

    roleText = addLabelledTextBox("Tomcat role name (for password protection only)");
    roleText.setText("toxygates-test");
    roleText.setEnabled(addNew);

    addCommands();
  }

  @Override
  protected void triggerEdit() {
    if (addNew) {
      Instance i = new Instance(idText.getValue(), commentArea.getValue(), new Date());
      AccessPolicy ap = policySelector.value();
      i.setAccessPolicy(ap, roleText.getText());
      maintenanceService.add(i, editCallback());
    } else {
      Window.alert("Implement me");
    }
  }
}
