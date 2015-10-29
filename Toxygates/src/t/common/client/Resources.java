package t.common.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ClientBundle.Source;

public interface Resources extends ClientBundle {
  @Source("images/16_info.png")
  ImageResource info();
  
  @Source("images/16_search.png")
  ImageResource magnify();
}
