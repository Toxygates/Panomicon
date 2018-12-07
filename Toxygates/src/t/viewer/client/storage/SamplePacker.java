package t.viewer.client.storage;

import javax.annotation.Nullable;

import t.common.shared.sample.Sample;
import t.model.SampleClass;

public class SamplePacker extends Packer<Sample> {
  private SampleClassPacker sampleClassPacker;

  public SamplePacker(SampleClassPacker sampleClassPacker) {
    this.sampleClassPacker = sampleClassPacker;
  }

  @Override
  public String pack(Sample sample) {
    final String sep = "$$$";
    StringBuilder sb = new StringBuilder();
    sb.append("Barcode_v3").append(sep);
    sb.append(sample.id()).append(sep);
    sb.append(sampleClassPacker.pack(sample.sampleClass())).append(sep);
    return sb.toString();
  }

  @Override
  public @Nullable Sample unpack(String string) throws UnpackInputException {
    String[] spl = string.split("\\$\\$\\$");
    if (spl.length != 3) {
      throw new UnpackInputException("Malformed serialized sample: wrong number of "
          + "$$$-separated tokens. Should be 3, but found " + spl.length + ".");
    }
    String v = spl[0];
    if (!v.equals("Barcode_v3")) {
      throw new UnpackInputException("Legacy data has been detected in your browser's storage. "
          + "Some of your older sample groups may not load properly.");
    }
    String id = spl[1];
    SampleClass sc = sampleClassPacker.unpack(spl[2]);
    return new Sample(id, sc);
  }
}
