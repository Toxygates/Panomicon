package t.common.testing;

import t.common.shared.DataSchema;

public class TestData {

  private final static DataSchema dataSchema = new TestSchema();

  public static DataSchema dataSchema() {
    return dataSchema;
  }

}
