package otgviewer.client.components;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.TextArea;

public class ResizableTextArea extends TextArea implements RequiresResize {
    public void onResize() {
        int height = getParent().getOffsetHeight();
        int width = getParent().getOffsetWidth();
        setSize(width + "px", height + "px");
    }	
}
