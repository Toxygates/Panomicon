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
		this.setStylePrimaryName("resizingListBox");
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
