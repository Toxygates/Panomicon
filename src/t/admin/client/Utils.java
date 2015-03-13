package t.admin.client;

import java.util.List;

import t.common.client.Command;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

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
