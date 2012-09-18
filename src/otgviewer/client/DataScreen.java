package otgviewer.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class DataScreen extends Screen {

	public static final String key = "data";
	private ExpressionTable et;
	public DataScreen(Screen parent) {
		super(parent, "View data", key, true);
	}
	
	public Widget content() {
		et = new ExpressionTable(null, (Window.getClientHeight() - 100) + "px");
		addListener(et);
		return et;		
	}
}
