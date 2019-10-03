package t.viewer.client.storage;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Sample;
import t.common.shared.sample.SampleGroup;
import t.viewer.client.ClientGroup;

/**
 * Like GroupPacker, but for ClientGroup. The logic is copy-pasted from
 * GroupPacker; the plan is to eventually just get rid of GroupPacker.
 */
public class ClientGroupPacker extends Packer<ClientGroup> {
  private SamplePacker samplePacker;
  private DataSchema schema;

  private static final String ACTIVE_GROUP_CODE = "Active Group";
  private static final String INACTIVE_GROUP_CODE = "Inactive Group";
  
  public ClientGroupPacker(SamplePacker samplePacker, DataSchema schema) {
    this.samplePacker = samplePacker;
    this.schema = schema;
  }

  @Override
  public String pack(ClientGroup group) {
    StringBuilder s = new StringBuilder();
    s.append(group.active ? ACTIVE_GROUP_CODE : INACTIVE_GROUP_CODE);
    s.append(":::");
    s.append(group.getName() + ":::"); // !!
    s.append(group.getColor() + ":::");
    for (Sample sample : group.samples()) {
      s.append(samplePacker.pack(sample));
      s.append("^^^");
    }
    return s.toString();
  }

  @Override
  public ClientGroup unpack(String string) throws UnpackInputException {
    if (string == null) {
      return null;
    }

    // This check for a legacy format is probably not necessary now that we have 
    // better exceptions downstream
    String[] spl = string.split("\\$\\$\\$");
    if (spl[0].equals("Barcode") || spl[0].equals("Barcode_v3")) {
      throw new UnpackInputException("Unsupported legacy column format: " + string);
    }

    String[] s1 = string.split(":::");
    if (s1.length != 4) {
      throw new UnpackInputException("Malformed serialized group: wrong number of "
          + ":::-separated tokens. Should be 4, but found " + s1.length + ".");
    }
    boolean active;
    if (s1[0] == ACTIVE_GROUP_CODE) {
      active = true;
    } else if (s1[0] == INACTIVE_GROUP_CODE) {
      active = false;
    } else {
      throw new UnpackInputException("Unsupported legacy column header: " + s1[0]);
    }
    
    String name = s1[1];
    String color = "";
    String barcodes = "";

    color = s1[2];
    barcodes = s1[3];
    if (SharedUtils.indexOf(SampleGroup.groupColors, color) == -1) {
      // replace the color if it is invalid.
      // this lets us safely upgrade colors in the future.
      color = SampleGroup.pickColor();
    }

    String[] s2 = barcodes.split("\\^\\^\\^");
    Sample[] bcs = new Sample[s2.length];
    for (int i = 0; i < s2.length; ++i) {
      bcs[i] = samplePacker.unpack(s2[i]);
    }
    // DataFilter useFilter = (bcs[0].getUnit().getOrgan() == null) ? filter : null;
    return new ClientGroup(schema, name, bcs, color, active);
  }
}
