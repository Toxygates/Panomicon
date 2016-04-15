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

package t.common.client.maintenance;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Handles the uploading of a number of files.
 */
abstract public class ItemUploader extends Composite {

  protected boolean completed;
  protected List<UploadWrapper> uploaders = new ArrayList<UploadWrapper>();

  public ItemUploader() {
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);
    makeGUI(vp);
  }

  protected void resetAll() {
    for (UploadWrapper u : uploaders) {
      u.reset();
    }
  }

  abstract protected void makeGUI(VerticalPanel vp);

  abstract protected boolean canProceed();

}
