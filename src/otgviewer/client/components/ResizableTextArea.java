package otgviewer.client.components;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.TextArea;

public class ResizableTextArea extends TextArea implements RequiresResize {
	
	@Override
    public void onResize() {
        int height = getParent().getOffsetHeight();
        int width = getParent().getOffsetWidth();
        if (width > 10 && height > 10) {
        	setSize((width -10) + "px", (height - 10) + "px");
        }
    }	
}
