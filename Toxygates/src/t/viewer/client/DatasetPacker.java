package t.viewer.client;

import t.common.shared.Dataset;

public class DatasetPacker extends Packer<Dataset> {
  @Override
  public String pack(Dataset dataset) {
    return dataset.getId();
  }

  @Override
  public Dataset unpack(String string) {
    return new Dataset(string, "", "", null, string, 0);
  }
}
