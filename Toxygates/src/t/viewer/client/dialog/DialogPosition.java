/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.viewer.client.dialog;

import com.google.gwt.user.client.Window;

public enum DialogPosition {
	Center {
		public int computeX(int dialogWidth) {
			return Window.getClientWidth()/2 - dialogWidth/2;
		}
		public int computeY(int dialogHeight) {
			if (isTallDialog(dialogHeight)) {
				return 50;
			} else {
				return Window.getClientHeight()/2 - dialogHeight/2;
			}
		}
	},
	Side {
		public int computeX(int dialogWidth) {
			return Window.getClientWidth() - dialogWidth - 50;
		}
		public int computeY(int dialogHeight) {
			return Center.computeY(dialogHeight);
		}
	};
	
	abstract public int computeX(int dialogWidth);
	abstract public int computeY(int dialogHeight);
	
	public static boolean isTallDialog(int dialogHeight) {
		return dialogHeight > Window.getClientHeight() - 100;
	}
}
