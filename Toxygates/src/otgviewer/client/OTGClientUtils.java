package otgviewer.client;

import t.viewer.client.Utils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class OTGClientUtils {

  private static Resources resources = GWT.create(Resources.class);

  public static Widget mkHelpButton(final TextResource helpText, final ImageResource helpImage) {
    PushButton i = new PushButton(new Image(resources.help()));
    i.setStylePrimaryName("slightlySpaced");
    i.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        Utils.showHelp(helpText, helpImage);
      }

    });
    return i;
  }


}
