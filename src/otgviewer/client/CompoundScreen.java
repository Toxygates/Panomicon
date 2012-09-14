package otgviewer.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CompoundScreen extends Screen {

	static String key = "compound";
	
	public CompoundScreen(Screen parent) {
		super(parent, "Compound selection", key);
	}
	
	public Widget content() {
		VerticalPanel vp = new VerticalPanel();
		loadState();
		
		final CompoundSelector cs = new CompoundSelector(chosenDataFilter, "Compounds");		
		this.addListener(cs);
		cs.setWidth("350px");
		vp.add(cs);
		
		Button b = new Button("Proceed");
		vp.add(b);
		b.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				chosenCompounds = cs.getCompounds();
				if (chosenCompounds.size() == 0) {
					Window.alert("Please choose at least one compound.");
				} else {
					storeState();
					History.newItem(ColumnScreen.key);
				}
			}
		});
		return vp;
		
	}
}
