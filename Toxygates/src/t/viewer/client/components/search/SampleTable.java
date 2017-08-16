package t.viewer.client.components.search;

import com.google.gwt.cell.client.TextCell;

import t.common.shared.sample.Sample;

public class SampleTable extends ResultTable<Sample> {
  private TextCell textCell = new TextCell();

  public SampleTable(Delegate delegate) {
    super(delegate);
  }

  @Override
  protected KeyColumn<Sample> makeColumn(String key, boolean numeric) {
    return new KeyColumn<Sample>(textCell, key, numeric) {
      @Override
      public String getData(Sample sample) {
        return sample.get(keyName);
      }
    };
  }
}