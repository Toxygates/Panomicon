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
package t.gwt.admin.client;

import java.util.Date;

import javax.annotation.Nullable;

import t.gwt.common.client.maintenance.ManagedItemEditor;
import t.gwt.common.client.maintenance.TaskCallback;
import t.shared.common.Platform;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.TextArea;

public class PlatformEditor extends ManagedItemEditor {

  private @Nullable PlatformUploader uploader;
  protected TextArea publicComments;

  protected final MaintenanceServiceAsync maintenanceService = GWT.create(MaintenanceService.class);
  
  public PlatformEditor(@Nullable Platform p, boolean addNew) {
    super(p, addNew);
    publicComments = addTextArea("Public comments");

    if (p != null) {
      publicComments.setValue(p.getPublicComment());
    }

    if (addNew) {
      uploader = new PlatformUploader();
      vp.add(uploader);
    }
    addCommands();
  }  

  @Override
  protected void triggerEdit() {
    Platform p = new Platform(idText.getValue(), 0, commentArea.getValue(), 
        new Date(), publicComments.getValue());
    if (addNew) {
      maintenanceService.addPlatformAsync(p, uploader.platformType(), new TaskCallback(
          logger, "Add platform", maintenanceService) {
        @Override
        protected void onCompletion() {
          onFinish();
          onFinishOrAbort();
        }
      });

    } else {
      maintenanceService.update(p, editCallback());
    }
  }

}
