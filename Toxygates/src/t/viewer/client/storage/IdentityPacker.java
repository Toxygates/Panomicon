package t.viewer.client.storage;

public class IdentityPacker extends Packer<String> {
  @Override
  public String pack(String entity) {
    return entity;
  }

  @Override
  public String unpack(String string) {
    return string;
  }
}
