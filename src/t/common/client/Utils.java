package t.common.client;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class Utils {

	public static Widget makeButtons(List<Command> commands) {
		HorizontalPanel buttons = new HorizontalPanel();
		buttons.setSpacing(4);
		for (final Command c: commands) {
			Button b = makeButton(c);			
			buttons.add(b);
		}
		return buttons;
	}
	
	public static Button makeButton(final Command c) {
		Button b = new Button(c.getTitle());
		b.addClickHandler(new ClickHandler() {				
			@Override
			public void onClick(ClickEvent event) {
				c.run();					
			}
		});
		return b;
	}
}
