package otgviewer.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

public class DataScreen extends Screen {

	public static final String key = "data";
	private ExpressionTable et;
	public DataScreen(Screen parent, MenuBar mb) {
		super(parent, "View data", key, mb, true);
		
	}
	
	public Widget content() {
		et = new ExpressionTable((Window.getClientHeight() - 100) + "px");
		addListener(et);
		
		MenuItem[] mis = et.menuItems();
		for (MenuItem mi: mis) {
			addMenu(mi);
		}
		
		return et;		
	}
}
