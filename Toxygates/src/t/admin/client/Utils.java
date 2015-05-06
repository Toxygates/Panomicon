package t.admin.client;

import com.google.gwt.user.client.ui.DialogBox;

public class Utils {
	
	static void showProgress(String title) {
		final DialogBox db = new DialogBox();
		ProgressDisplay pd = new ProgressDisplay(title) {
			@Override
			protected void onDone() {
				db.hide();			
			}
		};
		db.setWidget(pd);
		db.setText("Progress");
		db.show();		
	}
}
