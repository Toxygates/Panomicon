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
