package otgviewer.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

public class DataScreen extends Screen {

	public static final String key = "data";
	private ExpressionTable et;
	public DataScreen(Screen parent, ScreenManager man) {
		super(parent, "View data", key, true, man);
		
	}
	
	public Widget content() {
		et = new ExpressionTable((Window.getClientHeight() - 100) + "px");
		addListener(et);
		
		MenuItem[] mis = et.menuItems();
		for (MenuItem mi: mis) {
			addMenu(mi);
		}
		
		HorizontalPanel hp = new HorizontalPanel();
		Button b = new Button("Inspect pathological/chemical data");
		hp.add(b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				History.newItem(PathologyScreen.key);
			}
		});
		dockPanel.add(hp, DockPanel.SOUTH);
		
		return et;		
	}
	
	public void show() {
		super.show();
		//state has finished loading
		
		et.getExpressions(chosenProbes, false);		
		
	}
}
