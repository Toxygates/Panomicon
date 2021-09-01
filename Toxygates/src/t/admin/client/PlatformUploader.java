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

package t.admin.client;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import t.admin.shared.PlatformType;
import t.common.client.maintenance.ItemUploader;
import t.common.client.maintenance.UploadWrapper;
import t.shared.common.maintenance.MaintenanceConstants;

public class PlatformUploader extends ItemUploader {

  UploadWrapper platform;
  RadioButton affyRadio, tRadio, bioRadio;

  public PlatformUploader() {
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);
    platform =
        new UploadWrapper(this, "Platform definition (CSV/TSV)",
            MaintenanceConstants.platformPrefix, "tsv", "csv");

    vp.add(platform);

    Label l = new Label("File format");
    vp.add(l);
    affyRadio = makeRadio("type", "Affymetrix CSV");
    vp.add(affyRadio);
    tRadio = makeRadio("type", "T platform TSV");
    vp.add(tRadio);    
    bioRadio = makeRadio("type", "Biological data TSV");
    vp.add(bioRadio);
  }

  @Nullable PlatformType platformType() {
    if (affyRadio.getValue()) {
      return PlatformType.Affymetrix;      
    } else if (tRadio.getValue()) {
      return PlatformType.Standard;
    } else if (bioRadio.getValue()) {
      return PlatformType.Biological;
    } else {
      return null;
    }    
  }
  
  private RadioButton makeRadio(String group, String label) {
    RadioButton r = new RadioButton(group, label);    
    return r;
  }

  @Override
  protected boolean canProceed() {
    return (platform.hasFile() && platformType() != null);
  }
}
