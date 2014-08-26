package t.common.client.components;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;

/**
 * A list box that resizes itself vertically.
 * This is needed for Internet Explorer to handle vertically resizing
 * lists correctly. All other web browsers would have handled the standard ListBox
 * fine on vertical resize.
 */
public class ResizingListBox extends ListBox implements RequiresResize {

	private final int offset;
	private final static int MIN_ITEMS = 2;
	public ResizingListBox(int offset) {
		super();		
		this.offset = offset;
		this.setVisibleItemCount(MIN_ITEMS);
		this.setStyleName("resizingListBox");
	}
	
	@Override
	public void onResize() {
		int h = this.getParent().getOffsetHeight() - offset;		
		// 13 is defined as the font size of the items in the resizingListBox style.
		// It seems that whitespace is also inserted between items.
		int items = h/16;
		if (items > MIN_ITEMS) {
			this.setVisibleItemCount(items);
		}
	}
}
