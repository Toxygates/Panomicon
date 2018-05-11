/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import static t.common.shared.maintenance.MaintenanceConstants.*;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class BatchUploader extends ItemUploader {
  UploadWrapper metadata, data, calls;

  public BatchUploader(boolean full) {
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);

    metadata = new UploadWrapper(this, "Metadata file (TSV)" +
        (full ? "" : " (optional)"), metaPrefix, "tsv");
    uploaders.add(metadata);
    HorizontalPanel hp = new HorizontalPanel();
    hp.add(metadata);

    if (full) {
      data = new UploadWrapper(this, "Normalized data file (CSV)", dataPrefix, "csv");
      uploaders.add(data);
      hp.add(data);
      vp.add(hp);

      hp = new HorizontalPanel();
      hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      hp.setWidth("100%");
      calls =
          new UploadWrapper(this, "Affymetrix calls file (CSV) (optional)", callPrefix, "csv");
      uploaders.add(calls);
      hp.add(calls);
    }

    vp.add(hp);
  }

  @Override
  public boolean canProceed() {
    return metadata.hasFile()
        && (data == null || data.hasFile())
        && (calls == null || calls.hasFile()
            || Window.confirm("The Affymetrix calls file is missing. "
            + "Upload batch without calls data (all values present)?"));
  }
}
