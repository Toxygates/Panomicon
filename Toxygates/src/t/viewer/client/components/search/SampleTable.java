package t.viewer.client.components.search;

import t.common.shared.sample.Sample;
import t.model.sample.Attribute;

import com.google.gwt.cell.client.TextCell;

public class SampleTable extends ResultTable<Sample> {
  private TextCell textCell = new TextCell();

  public SampleTable(Delegate delegate) {
    super(delegate);
  }

  @Override
  protected AttributeColumn<Sample> makeColumn(Attribute attribute, boolean numeric) {
    return new AttributeColumn<Sample>(textCell, attribute, numeric) {
      @Override
      public String getData(Sample sample) {
        return sample.get(attribute);
      }
    };
  }
}
