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

import static t.common.shared.maintenance.MaintenanceConstants.platformPrefix;
import t.common.client.maintenance.ItemUploader;
import t.common.client.maintenance.UploadWrapper;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PlatformUploader extends ItemUploader {

  UploadWrapper platform;
  RadioButton affyRadio, tRadio;

  protected void makeGUI(VerticalPanel vp) {
    platform =
        new UploadWrapper(this, "Platform definition (CSV/TSV)", platformPrefix, "tsv", "csv");

    vp.add(platform);

    Label l = new Label("File format");
    vp.add(l);
    affyRadio = makeRadio("type", "Affymetrix CSV");
    vp.add(affyRadio);
    tRadio = makeRadio("type", "T platform TSV");
    vp.add(tRadio);
  }

  private RadioButton makeRadio(String group, String label) {
    RadioButton r = new RadioButton(group, label);    
    return r;
  }

  protected boolean canProceed() {
    return (platform.hasFile() && (affyRadio.getValue() || tRadio.getValue()));
  }
}
