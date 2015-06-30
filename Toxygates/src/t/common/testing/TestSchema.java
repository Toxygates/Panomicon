package t.common.testing;

import t.common.shared.DataSchema;
import t.common.shared.SampleClass;

@SuppressWarnings("serial")
public class TestSchema extends DataSchema {

  public TestSchema() {
  }

  @Override
  public String[] sortedValues(String parameter) throws Exception {
    throw new Exception("Implement me");
  }

  @Override
  public String majorParameter() { return "major"; }

  @Override
  public String mediumParameter() { return "medium"; }

  @Override
  public String minorParameter() { return "minor"; }

  @Override
  public String timeParameter() { return "minor"; }

  @Override
  public String timeGroupParameter() { return "medium"; }
  
  @Override
  public String[] macroParameters() {
    return new String[] { "macro1", "macro2" };
  }

  @Override
  public String title(String parameter) {
    return "T:" + parameter;
  }

  @Override
  public int numDataPointsInSeries(SampleClass sc) {
    // TODO Auto-generated method stub
    return 0;
  }

}
