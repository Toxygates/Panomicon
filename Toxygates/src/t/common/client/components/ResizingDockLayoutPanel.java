/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.common.client.components;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A DockLayoutPanel that resizes some of its children automatically.
 * The unit is always Unit.PX.
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
