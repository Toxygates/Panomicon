package otgviewer.client.dialog;

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
