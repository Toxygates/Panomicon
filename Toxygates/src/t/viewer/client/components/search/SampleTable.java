package t.viewer.client.components.search;

import com.google.gwt.cell.client.TextCell;

import t.common.shared.sample.Sample;
import t.viewer.client.table.TooltipColumn;

public class SampleTable extends ResultTable<Sample> {
  private TextCell textCell = new TextCell();

  public SampleTable(Delegate delegate) {
    super(delegate);
  }

  private TooltipColumn<Sample> makeColumn(String key) {
    return new KeyColumn<Sample>(textCell, key) {
      @Override
      public String getData(Sample sample) {
        return sample.get(keyName);
      }
    };
  }

  @Override
  protected TooltipColumn<Sample> makeBasicColumn(String key) {
    return makeColumn(key);
  }

  @Override
  protected TooltipColumn<Sample> makeNumericColumn(String key) {
    return makeColumn(key);
  }
}
