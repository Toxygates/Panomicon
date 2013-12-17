package otgviewer.client.components;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;

/**
 * A list box that resizes itself vertically.
 */
public class ResizingListBox extends ListBox implements RequiresResize {

	private final int offset;
	private final static int MIN_ITEMS = 2;
	public ResizingListBox(int offset) {
		super();
		this.offset = offset;
		this.setVisibleItemCount(MIN_ITEMS);
	}
	
	@Override
	public void onResize() {
		int h = this.getParent().getOffsetHeight() - offset;
		int items = h/22;
		if (items > MIN_ITEMS) {
			this.setVisibleItemCount(items);
		}
	}
}
