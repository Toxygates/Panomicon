package otgviewer.client;

import otgviewer.client.Utils;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the first screen, where a dataset can be selected.
 */
public class StartScreen extends Screen {
	protected static Resources resources = GWT.create(Resources.class);
	
	public static String key = "st";	
	
	public StartScreen(ScreenManager man) {
		super("Start", key, false, man);		
	}

	final private HTML welcomeHtml = new HTML();

	public Widget content() {
		HorizontalPanel hp = Utils.mkWidePanel();
		hp.setHeight("100%");

		hp.add(welcomeHtml);
		welcomeHtml.setWidth("40em");
		Utils.loadHTML("../shared/" + manager.instanceName() + "/welcome.html", 
				new Utils.HTMLCallback() {
			@Override
			protected void setHTML(String html) {
				welcomeHtml.setHTML(html);
			}
		});

		return Utils.makeScrolled(hp);
	}

	@Override
	public String getGuideText() {
		return "Welcome.";
	}
}
