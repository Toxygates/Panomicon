package bioweb.client.components;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * A HorizontalPanel that passes the resize signal on to its children.
 *
 */
public class ResizingHorizontalPanel extends HorizontalPanel implements RequiresResize, ProvidesResize {

	@Override
	public void onResize() {
		for (Widget w: getChildren()) {
			if (w instanceof RequiresResize) {
				((RequiresResize) w).onResize();
			}
		}
	}

}
