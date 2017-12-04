/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client.maintenance;

import com.google.gwt.user.client.ui.*;

import gwtupload.client.*;
import gwtupload.client.IUploader.*;

/**
 * Handles the uploading of a single file.
 */
public class UploadWrapper extends Composite {
  Uploader u;
  boolean finished;
  Label statusLabel = new Label();
  VerticalPanel vp = new VerticalPanel();
  ItemUploader manager;

  public UploadWrapper(ItemUploader manager, String description, String prefix, String... extensions) {
    this.manager = manager;
    initWidget(vp);
    Label l = new Label(description);
    vp.add(l);
    vp.addStyleName("uploader");
    vp.setHeight("80px");
    vp.setWidth("250px");

    u = new SingleUploader();
    u.setFileInputPrefix(prefix);
    u.setValidExtensions(extensions);
    u.setAutoSubmit(true);

    u.addOnStartUploadHandler(new OnStartUploaderHandler() {
      @Override
      public void onStart(IUploader uploader) {
        setFailure();
        statusLabel.setText("In progress");
      }
    });
    u.addOnFinishUploadHandler(new OnFinishUploaderHandler() {
      @Override
      public void onFinish(IUploader uploader) {
        setFinished();
      }
    });
    u.addOnCancelUploadHandler(new OnCancelUploaderHandler() {
      @Override
      public void onCancel(IUploader uploader) {
        setFailure();
      }
    });
    vp.add(u);
    vp.add(statusLabel);
    setFailure();
  }

  void setFinished() {
    finished = true;
    statusLabel.addStyleDependentName("success");
    statusLabel.removeStyleDependentName("failure");
    statusLabel.setText("OK");
  }

  void setFailure() {
    finished = false;
    statusLabel.addStyleDependentName("failure");
    statusLabel.removeStyleDependentName("success");
    statusLabel.setText("Please upload a file");
  }

  public boolean hasFile() {
    return finished;
  }

  void reset() {
    setFailure();
  }
}
