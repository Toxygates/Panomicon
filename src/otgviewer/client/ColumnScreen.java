package otgviewer.client;

import java.util.Arrays;

import otgviewer.shared.DataColumn;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ColumnScreen extends Screen {
	public static String key = "columns";
	
	
	public ColumnScreen(Screen parent) {
		super(parent, "Column definitions", key, true);
	}
	
	public Widget content() {
		VerticalPanel vp = new VerticalPanel();
		HorizontalPanel hp = new HorizontalPanel();
		vp.add(hp);
		CompoundSelector cs = new CompoundSelector(chosenDataFilter, "1. Compounds");
		this.addListener(cs);
		hp.add(cs);
		final GroupInspector gi = new GroupInspector(cs);
		this.addListener(gi);
		cs.addListener(gi);
		hp.add(gi);
		
		Button b = new Button("Next: Select probes");		
		b.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				chosenColumns = Arrays.asList(gi.getGroups().values().toArray(new DataColumn[0]));
				storeState();
				History.newItem(ProbeScreen.key);				
			}
		});
		dockPanel.add(b, DockPanel.SOUTH);
		
		return vp;
	}
	
}
