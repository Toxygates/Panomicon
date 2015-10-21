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

import t.common.shared.Dataset;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

public class DatasetEditor extends ManagedItemEditor {

  private final TextBox descText;
  protected TextArea publicComments;

  public DatasetEditor(@Nullable Dataset d, boolean addNew) {
    super(d, addNew);

    descText = addLabelledTextBox("Description");
    publicComments = addTextArea("Public comments");

    if (d != null) {
      descText.setValue(d.getDescription());
      publicComments.setValue(d.getPublicComment());
    }
    addCommands();
  }

  @Override
  protected void triggerEdit() {
    Dataset d =
        new Dataset(idText.getValue(), descText.getValue(), 
            commentArea.getValue(), new Date(), 
            publicComments.getValue());
    if (addNew) {
      maintenanceService.add(d, editCallback());
    } else {      
      maintenanceService.update(d, editCallback());
    }
  }
}
