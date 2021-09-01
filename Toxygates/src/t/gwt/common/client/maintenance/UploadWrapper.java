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

package t.gwt.common.client.maintenance;

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

  private static Integer uploadCount = 0;
  final private String purePrefix;

  public UploadWrapper(ItemUploader manager, String description, String prefix, String... extensions) {
    this.manager = manager;
    this.purePrefix = prefix;
    initWidget(vp);
    Label l = new Label(description);
    vp.add(l);
    vp.addStyleName("uploader");
    vp.setHeight("80px");
    vp.setWidth("250px");

    u = new SingleUploader();
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

  /*
    By incrementing the file prefix every time we upload another file,
    we can distinguish them more easily on the server side if several files are uploaded
    through the same Uploader.
   */
  synchronized protected void incrementPrefix() {
    uploadCount += 1;
    //String.format is not in GWT
    StringBuilder r = new StringBuilder();
    r.append(purePrefix + "-");
    if (uploadCount < 100) {
      r.append('0');
    }
    if (uploadCount < 10) {
      r.append('0');
    }
    r.append(uploadCount);
    u.setFileInputPrefix(r.toString());
  }

  void setFinished() {
    finished = true;
    statusLabel.addStyleDependentName("success");
    statusLabel.removeStyleDependentName("failure");
    statusLabel.setText("OK");
    incrementPrefix();
  }

  void setFailure() {
    finished = false;
    statusLabel.addStyleDependentName("failure");
    statusLabel.removeStyleDependentName("success");
    statusLabel.setText("Please upload a file");
    incrementPrefix();
  }

  public boolean hasFile() {
    return finished;
  }

  void reset() {
    setFailure();
  }
}
