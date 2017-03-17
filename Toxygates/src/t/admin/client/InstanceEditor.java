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

import javax.annotation.Nullable;

import t.common.client.components.ItemSelector;
import t.common.client.maintenance.AccessPolicy;
import t.common.client.maintenance.ManagedItemEditor;
import t.common.shared.maintenance.Instance;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class InstanceEditor extends ManagedItemEditor {

  private final TextBox roleText;
  private final ItemSelector<AccessPolicy> policySelector;

  protected final MaintenanceServiceAsync maintenanceService = GWT.create(MaintenanceService.class);
  
  public InstanceEditor(@Nullable Instance i, boolean addNew) {
    super(i, addNew);

    Label l = new Label("Access policy");
    vp.add(l);

    //TODO set values for these fields and keep them in sync
    policySelector = new ItemSelector<AccessPolicy>() {
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
    Instance i = new Instance(idText.getValue(), commentArea.getValue(), new Date());
    if (addNew) {
      AccessPolicy ap = policySelector.value();
      i.setAccessPolicy(ap, roleText.getText());
      maintenanceService.add(i, editCallback());
    } else {
      maintenanceService.update(i, editCallback());
    }
  }
}
