package t.viewer.client.storage;

import javax.annotation.Nullable;

import t.viewer.shared.mirna.MirnaSource;

public class MirnaSourcePacker extends Packer<MirnaSource> {

  @Override
  public String pack(MirnaSource mirnaSource) {
    return mirnaSource.id() + "^^^" + mirnaSource.limit();
  }

  @Nullable
  Double parseNullableDouble(String data) {
    if (data.equals("null")) {
      return null;
    }
    return Double.parseDouble(data);
  }

  @Override
  public MirnaSource unpack(String string) throws UnpackInputException {
    String[] splits = string.split("\\^\\^\\^");
    Double limit;
    
    if (splits.length == 2) {
      try {
        limit = parseNullableDouble(splits[1]);
        return new MirnaSource(splits[0], "", false, limit, 0, null, null, null);
      } catch (NumberFormatException e) {
        throw new UnpackInputException("Could not parse miRNA source limit: " + splits[1]);
      }
    } else {
      throw new UnpackInputException("Malformed serialized miRNA source: expected 2 ^^^-separated " +
          " tokens but found " + splits.length + " in: " + string);
    }
  }
}
