package otgviewer.client.components;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.TextArea;

public class ResizableTextArea extends TextArea implements RequiresResize {
	
	public ResizableTextArea(int widthDelta, int heightDelta) {
		this.widthDelta = widthDelta;
		this.heightDelta = heightDelta;
	}
	
	private final int widthDelta, heightDelta;
	
	@Override
    public void onResize() {
        int width = getParent().getOffsetWidth();
        int height = getParent().getOffsetHeight();
        if (width > widthDelta && height > heightDelta) {
        	setSize((width - widthDelta) + "px", (height - heightDelta) + "px");
        }
    }	
}
