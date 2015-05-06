package otgviewer.client.components;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * A layout panel that tries to keep a single widget at a fixed width, centered,
 * filling all vertical space.
 *
 */
public class FixedWidthLayoutPanel extends LayoutPanel {

	private int fixedWidth;
	private Widget inner;
	public FixedWidthLayoutPanel(Widget inner, int fixedWidth, int margin) {
		add(inner);
		this.inner = inner;
		setWidgetTopBottom(inner, margin, Unit.PX, margin, Unit.PX);
		this.fixedWidth = fixedWidth;
		adjustWidth();
	}

	private void adjustWidth() {
		int w = getOffsetWidth();
		int dist = w/2 - fixedWidth / 2;
		if (dist < 0) {
			dist = 0;
		}
		setWidgetLeftRight(inner, dist, Unit.PX, dist, Unit.PX);		
	}
	
	@Override
	public void onResize() {
		super.onResize();
		adjustWidth();			
		if (inner instanceof RequiresResize) {
			((RequiresResize) inner).onResize();
		}
	}

}
