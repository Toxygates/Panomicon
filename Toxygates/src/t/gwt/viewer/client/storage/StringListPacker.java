package t.gwt.viewer.client.storage;

import t.shared.viewer.ItemList;
import t.shared.viewer.StringList;

public class StringListPacker extends Packer<StringList> {

  private Packer<ItemList> itemListPacker = new ItemListPacker();
  
  @Override
  public String pack(StringList stringList) {
    return itemListPacker.pack(stringList);
  }

  @Override
  public StringList unpack(String string) throws UnpackInputException {
    ItemList itemList = itemListPacker.unpack(string);
    if (!(itemList instanceof StringList)) {
      throw new UnpackInputException("StringListPacker tried to unpack a non-StringList ItemList.");
    } else {
      return (StringList) itemList;
    }
  }
}
