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

package t.common.shared.maintenance;

@SuppressWarnings("serial")
public class BatchUploadException extends MaintenanceException {
  // These fields should be set by the constructor and not changed again
  public boolean idWasBad;
  public boolean metadataWasBad;
  public boolean normalizedDataWasBad;

  /**
   * GWT constructor
   */
  public BatchUploadException() {
    idWasBad = false;
    metadataWasBad = false;
    normalizedDataWasBad = false;
  }

  public BatchUploadException(String message, boolean _idWasBad, boolean _metadataWasBad,
      boolean _normalizedDataWasBad) {
    super(message);
    idWasBad = _idWasBad;
    metadataWasBad = _metadataWasBad;
    normalizedDataWasBad = _normalizedDataWasBad;
  }

  public static BatchUploadException badID(String message) {
    return new BatchUploadException(message, true, false, false);
  }

  public static BatchUploadException badMetaData(String message) {
    return new BatchUploadException(message, false, true, false);
  }

  public static BatchUploadException badNormalizedData(String message) {
    return new BatchUploadException(message, false, false, true);
  }

}
