package t.viewer.client;

import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;

/**
 * Like a group, but encodes the notion of whether it is active or inactive,
 * which is relevant for the web client.
 */
@SuppressWarnings("serial")
public class ClientGroup extends Group {
  public boolean active;
  
  public ClientGroup(DataSchema schema, String name, Sample[] barcodes, String color,
      boolean active) {
    super(schema, name, barcodes, color);
    this.active = active;
    _units = Unit.formUnits(schema, barcodes);
  }
  
  public ClientGroup(DataSchema schema, String name, Unit[] units, boolean active) {
    super(schema, name, Unit.collectBarcodes(units));
    this.active = active;
    _units = units;
  }
}
