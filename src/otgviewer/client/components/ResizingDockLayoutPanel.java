package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A DockLayoutPanel that resizes some of its children automatically.
 * The unix is always Unit.PX.
 */
public class ResizingDockLayoutPanel extends DockLayoutPanel {

	public ResizingDockLayoutPanel() {
		super(Unit.PX);
	}

	@Override
	public void onResize() {
		super.onResize();
		for (Widget w: getChildren()) {
			Direction d = getWidgetDirection(w);
			switch(d) {
			case NORTH:
				// fall through
			case SOUTH:
				setWidgetSize(w, w.getOffsetHeight());
				break;
			case WEST:
				// fall through
			case EAST:
				setWidgetSize(w, w.getOffsetWidth());
			default:
				break;
			}			
		}
	}

}
